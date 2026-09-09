package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogSearchParams;
import by.AntonDemchuk.ai_pdf_service.entity.*;
import by.AntonDemchuk.ai_pdf_service.mapper.auditLog.AuditLogReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.auditLog.AuditLogRepository;
import by.AntonDemchuk.ai_pdf_service.repository.auditLog.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private final AuditLogReadMapper auditLogReadMapper;

    public void log(User user, AuditLogAction action, String message, AuditLogStatus status) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .message(message)
                .status(status)
                .createdAt(ZonedDateTime.now())
                .build());
    }

    public void log(User user, PDFFile file, AuditLogAction action, String message, AuditLogStatus status) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .pdfFile(file)
                .action(action)
                .message(message)
                .status(status)
                .createdAt(ZonedDateTime.now())
                .build());
    }

    public void log(User user, PDFFile file, ProcessingJob processingJob, AuditLogAction action, String message, AuditLogStatus status) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .pdfFile(file)
                .processingJob(processingJob)
                .action(action)
                .message(message)
                .status(status)
                .createdAt(ZonedDateTime.now())
                .build());
    }

    private final SharedService sharedService;

    @Transactional(readOnly = true)
    public PageDTO<AuditLogReadDTO> findAll(AuditLogSearchParams params, Pageable pageable) {

        Specification<AuditLog> spec = AuditLogSpecification.buildSpecification(params);

        return auditLogReadMapper.toPageDto(auditLogRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public PageDTO<AuditLogReadDTO> findMyLogs(
            AuditLogStatus status,
            AuditLogAction action,
            ZonedDateTime fromDate,
            ZonedDateTime toDate,
            Pageable pageable) {

        Long currentUserId = sharedService.getCurrentUser().getId();

        AuditLogSearchParams params = AuditLogSearchParams.builder()
                .userId(currentUserId)
                .status(status)
                .action(action)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        Specification<AuditLog> spec = AuditLogSpecification.buildSpecification(params);
        return auditLogReadMapper.toPageDto(auditLogRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public PageDTO<AuditLogReadDTO> findByFile(
            Long fileId,
            AuditLogStatus status,
            AuditLogAction action,
            Pageable pageable) {

        Long currentUserId = sharedService.getCurrentUser().getId();

        AuditLogSearchParams params = AuditLogSearchParams.builder()
                .pdfFileId(fileId)
                .userId(currentUserId)
                .status(status)
                .action(action)
                .build();

        Specification<AuditLog> spec = AuditLogSpecification.buildSpecification(params);
        return auditLogReadMapper.toPageDto(auditLogRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public PageDTO<AuditLogReadDTO> findByUser(
            Long userId,
            AuditLogStatus status,
            AuditLogAction action,
            Pageable pageable) {

        AuditLogSearchParams params = AuditLogSearchParams.builder()
                .userId(userId)
                .status(status)
                .action(action)
                .build();

        Specification<AuditLog> spec = AuditLogSpecification.buildSpecification(params);
        return auditLogReadMapper.toPageDto(auditLogRepository.findAll(spec, pageable));
    }
}
