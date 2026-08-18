package com.example.filestorage.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;


public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> from(Page<E> springPage, Function<E, T> mapper) {
        return new PageResponse<>(
                springPage.getContent().stream().map(mapper).toList(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }
}
