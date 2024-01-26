package com.si.googleads.exceptions;

import com.mongodb.MongoException;

public class DatabaseException extends MongoException {
    public DatabaseException(String message) {
        super(message);
    }
}
