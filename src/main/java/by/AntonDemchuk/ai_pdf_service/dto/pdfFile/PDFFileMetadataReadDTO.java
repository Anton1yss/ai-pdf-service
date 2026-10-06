package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PDFFileMetadataReadDTO {
    private String title;
    private String author;
    private String subject;
    private String keywords;

    private String creator;
    private String producer;

    private ZonedDateTime creationDate;
    private ZonedDateTime modificationDate;

    private String pdfVersion;
    private Integer pageCount;
}