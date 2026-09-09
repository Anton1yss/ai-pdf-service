package by.AntonDemchuk.ai_pdf_service.dto.auditLog;

import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class AuditLogSearchParams {
    private final Long userId;
    private final Long pdfFileId;
    private final AuditLogStatus status;
    private final AuditLogAction action;
    private final ZonedDateTime fromDate;
    private final ZonedDateTime toDate;
}
