package com.hust.commonlibrary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListResponse<T> {
    List<T> content;
    Integer pageNumber;
    Integer pageSize;
    long totalElements;
    int totalPages;
    boolean isLast;
    Map<String, Object> meta;

    public <E> ListResponse(List<T> content, Page<E> page) {
        this.content = content;
        this.pageNumber = page.getNumber() + 1;
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.isLast = page.isLast();
    }

    public <E> ListResponse(List<T> content, Page<E> page, Map<String, Object> meta) {
        this(content, page);
        this.meta = meta;
    }

    public static <T, E> ListResponse<T> of(List<T> content, Page<E> page) {
        return new ListResponse<>(content, page);
    }

    public static <T, E> ListResponse<T> of(List<T> content, Page<E> page, Map<String, Object> meta) {
        return new ListResponse<>(content, page, meta);
    }

    public static <T> ListResponse<T> of(List<T> content, int pageNumber, int pageSize,
                                         long totalElements, int totalPages, boolean isLast) {
        return ListResponse.<T>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(isLast)
                .build();
    }

    public static <T> ListResponse<T> of(List<T> content, int pageNumber, int pageSize,
                                         long totalElements, int totalPages, boolean isLast, Map<String, Object> meta) {
        return ListResponse.<T>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(isLast)
                .meta(meta)
                .build();
    }
}
