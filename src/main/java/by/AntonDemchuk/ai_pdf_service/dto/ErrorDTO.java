package by.AntonDemchuk.ai_pdf_service.dto;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class ErrorDTO {
    int code;
    String message;
    List<String> errors;
}