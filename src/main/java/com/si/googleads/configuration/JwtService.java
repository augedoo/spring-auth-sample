package com.si.googleads.configuration;

import com.si.googleads.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtService {
  @Value("${jwt_secret}")
  private String secretKey;
  @Value("${jwt_expiration_in_days}")
  private long jwtExpiration;
  @Value("${jwt_refresh_expiration_in_days}")
  private long refreshExpiration;

  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  public Date getExpirationDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(User user) {
    return generateToken(new HashMap<>(), user);
  }

  public String generateToken(
          Map<String, Object> extraClaims,
          User user) {
    return buildToken(extraClaims, user, jwtExpiration);
  }

  public String generateRefreshToken(
          User user) {
    return buildToken(new HashMap<>(), user, refreshExpiration);
  }

  public boolean isTokenValid(String token) {
    return isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return getExpiration(token).before(new Date());
  }

  private Date getExpiration(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts
            .parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
  }

  private Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private String buildToken(
          Map<String, Object> extraClaims,
          User user,
          long expiration) {
    return Jwts
            .builder()
            .setClaims(extraClaims)
            .setSubject(user.getEmail())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
  }

  // validate token
  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = getUsernameFromToken(token);
    return (username.equals(userDetails.getUsername()) && isTokenExpired(token));
  }

}
