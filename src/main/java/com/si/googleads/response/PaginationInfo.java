package com.si.googleads.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaginationInfo {
    private long totalItems;
    private int currentPage;
    private int totalPages;
}
