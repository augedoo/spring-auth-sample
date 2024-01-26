package com.si.googleads.helpers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AppHelpers {
    public static String formatDate (LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return date.format(formatter);
    }
}
