package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PDFFileSummarizeResponseDTO {
    private Long fileId;
    private String response;
}