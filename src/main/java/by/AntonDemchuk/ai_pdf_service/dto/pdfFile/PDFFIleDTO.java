package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;


@Data
@Builder
public class PDFFIleDTO {
    private String name;
    private String s3Key;
    private String originalS3Key;
    private ZonedDateTime createdAt;
    private Long version;
    private Long userId;
}
