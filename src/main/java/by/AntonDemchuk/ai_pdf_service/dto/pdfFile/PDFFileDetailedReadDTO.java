package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.PDFEncryptionSettings;
import lombok.*;

import java.time.ZonedDateTime;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class PDFFileDetailedReadDTO {
    private Long id;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private Long version;
    private String preSignedURL;
    private UserReadDTO user;
    private PDFEncryptionSettings encryptionSettings;
}