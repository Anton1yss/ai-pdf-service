package by.AntonDemchuk.ai_pdf_service.dto.processingJob;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class ProcessingJobDTO {
    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;
}
