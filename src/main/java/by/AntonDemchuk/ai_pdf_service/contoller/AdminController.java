package by.AntonDemchuk.ai_pdf_service.contoller;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogSearchParams;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import by.AntonDemchuk.ai_pdf_service.service.AuditLogService;
import by.AntonDemchuk.ai_pdf_service.service.PDFFileService;
import by.AntonDemchuk.ai_pdf_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Controller")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogService auditLogService;
    private final UserService userService;
    private final PDFFileService pdfFileService;

    /* AUDIT LOG */
    @GetMapping("/auditLog")
    @Operation(summary = "Search audit logs", description = "Returns a paginated list of audit logs. Supports filtering by user, PDF file, status, action and date range. This endpoint is available only to administrators.")
    public ResponseEntity<PageDTO<AuditLogReadDTO>> findAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long pdfFileId,
            @RequestParam(required = false) AuditLogStatus status,
            @RequestParam(required = false) AuditLogAction action,
            @RequestParam(required = false) ZonedDateTime fromDate,
            @RequestParam(required = false) ZonedDateTime toDate,
            @ParameterObject @PageableDefault(size = 5, page = 0) Pageable pageable) {

        AuditLogSearchParams params = AuditLogSearchParams.builder()
                .userId(userId)
                .pdfFileId(pdfFileId)
                .status(status)
                .action(action)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        return ResponseEntity.ok(auditLogService.findAll(params, pageable));
    }

    @GetMapping("/auditLog/user/{userId}")
    @Operation(summary = "Get audit logs for a User", description = "Returns a paginated list of audit logs associated with the specified User. Results can be filtered by status and action.")
    public ResponseEntity<PageDTO<AuditLogReadDTO>> findByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) AuditLogStatus status,
            @RequestParam(required = false) AuditLogAction action,
            @ParameterObject @PageableDefault(size = 5, page = 0) Pageable pageable) {

        return ResponseEntity.ok(auditLogService.findByUser(userId, status, action, pageable));
    }

    @GetMapping("/auditLog/file/{fileId}")
    @Operation(summary = "Get audit logs for a PDF file", description = "Returns a paginated list of audit logs associated with the specified PDF file. Results can be filtered by status and action.")
    public ResponseEntity<PageDTO<AuditLogReadDTO>> findByFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) AuditLogStatus status,
            @RequestParam(required = false) AuditLogAction action,
            @ParameterObject @PageableDefault(size = 5, page = 0) Pageable pageable) {

        return ResponseEntity.ok(auditLogService.findByFile(fileId, status, action, pageable));
    }

    /* USER */
    @GetMapping("/user")
    @Operation(summary = "Get all users", description = "Returns a paginated list of all registered users.")
    public ResponseEntity<PageDTO<UserReadDTO>> getAllUsers(
            @ParameterObject @PageableDefault(size = 5, page = 0, sort = {}) Pageable pageable) {

        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user by ID", description = "Returns user information identified by the specified user ID.")
    public ResponseEntity<UserReadDTO> getUser(@PathVariable Long userId){
        return ResponseEntity.ok(userService.findById(userId));
    }

    /* PDF File */

    /// TODO: add spec
    @RequestMapping(value="/pdfFile", method=RequestMethod.GET)
    @Operation(summary = "Get all PDF files", description = "Returns a page of all PDF files")
    public ResponseEntity<PageDTO<PDFFileReadDTO>> findAllDocuments(
            @ParameterObject @PageableDefault(size = 5, page = 0, sort = {}) Pageable pageable) {

        return ResponseEntity.ok(pdfFileService.findAllPDFFiles(pageable));
    }

}
