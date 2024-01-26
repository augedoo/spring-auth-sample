package com.si.googleads.util;

import org.springframework.beans.BeanUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AppUtils {

    public static <T, R> T entityToDto(R entity, Class<T> dtoClass) {
        T dtoInstance;
        try {
            dtoInstance = dtoClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(entity, dtoInstance);
        } catch (Exception e) {
            throw new RuntimeException("Entity to Dto conversion error. Failed to create an instance of the DTO class.", e);
        }
        return dtoInstance;
    }

    public static <T, R> T dtoToEntity(R dto, Class<T> entityClass) {
        T entityInstance;
        try {
            entityInstance = entityClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(dto, entityInstance);
        } catch (Exception e) {
            throw new RuntimeException("Dto to Entity conversion error. Failed to create an instance of the entity class", e);
        }
        return entityInstance;
    }

    public static <T, R> List<T> entityListToDtoList(List<R> entities, Class<T> dtoClass) {
        return entities.stream()
                .map(entity -> entityToDto(entity, dtoClass))
                .collect(Collectors.toList());
    }

    public static String formatDateString(String inputDate) {
        try {
            Date date = new SimpleDateFormat("yyyyMMdd").parse(inputDate);
            return new SimpleDateFormat("yyyy-MM-dd").format(date);
        } catch (ParseException e) {
            throw new RuntimeException("Error formatting date string", e);
        }
    }
}
