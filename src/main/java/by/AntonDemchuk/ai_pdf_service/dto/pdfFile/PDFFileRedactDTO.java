package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class PDFFileRedactDTO {
    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;
}
