package by.AntonDemchuk.ai_pdf_service.contoller;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import by.AntonDemchuk.ai_pdf_service.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auditLog")
@Tag(name = "Audit Log Controller")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/my")
    @Operation(summary = "Get my audit logs", description = "Returns a paginated list of audit logs associated with the currently authenticated user. Results can be filtered by status, action and date range.")
    public ResponseEntity<PageDTO<AuditLogReadDTO>> findMy(
            @RequestParam(required = false) AuditLogStatus status,
            @RequestParam(required = false) AuditLogAction action,
            @RequestParam(required = false) ZonedDateTime fromDate,
            @RequestParam(required = false) ZonedDateTime toDate,
            @ParameterObject @PageableDefault(size = 5, page = 0) Pageable pageable) {

        return ResponseEntity.ok(auditLogService.findMyLogs(status, action, fromDate, toDate, pageable));
    }
}
