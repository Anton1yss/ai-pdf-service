package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import lombok.*;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class PDFFileMetadataDTO {
    private String title;
    private String author;
    private String subject;
    private String keywords;
}