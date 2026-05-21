package com.gametrend.agent.admin.common;

import java.util.List;

public record AdminPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> AdminPageResponse<T> of(List<T> filteredItems, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, filteredItems.size());
        int toIndex = Math.min(fromIndex + safeSize, filteredItems.size());
        int totalPages = filteredItems.isEmpty()
                ? 0
                : (int) Math.ceil((double) filteredItems.size() / safeSize);
        return new AdminPageResponse<>(
                List.copyOf(filteredItems.subList(fromIndex, toIndex)),
                safePage,
                safeSize,
                filteredItems.size(),
                totalPages
        );
    }
}
