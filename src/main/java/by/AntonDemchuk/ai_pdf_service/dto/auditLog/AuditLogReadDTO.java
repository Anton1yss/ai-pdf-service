package by.AntonDemchuk.ai_pdf_service.dto.auditLog;

import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import lombok.*;

import java.time.ZonedDateTime;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class AuditLogReadDTO {
    private Long id;
    private final UserReadDTO user;
    private final PDFFileReadDTO pdfFile;
    private final String message;
    private final AuditLogStatus status;
    private final AuditLogAction action;
    private final ZonedDateTime fromDate;
    private final ZonedDateTime toDate;
    private final ZonedDateTime createdAt;
}
