package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PDFFileSummarizeDTO {

    @NotBlank(message = "Prompt cannot be empty")
    @Size(max = 1000)
    private String prompt;

}