package by.AntonDemchuk.ai_pdf_service.mapper;


import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import org.springframework.data.domain.Page;

import java.util.stream.Collectors;

public interface BaseMapper<T, F> {

    T toEntity(F dto);

    F toDto(T entity);

    default PageDTO<F> toPageDto(Page<T> page) {
        return PageDTO.<F>builder()
                .pageNumber(page.getNumber())
                .content(page.getContent().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()))
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }

}
