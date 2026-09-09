package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobDTO;
import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.PDFFile;
import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJob;
import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJobStatus;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.mapper.processingJob.ProcessingJobMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.processingJob.ProcessingJobReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.PDFFileRepository;
import by.AntonDemchuk.ai_pdf_service.repository.ProcessingJobRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProcessingJobService {

    private final ProcessingJobMapper processingJobMapper;
    private final ProcessingJobReadMapper processingJobReadMapper;

    private final ProcessingJobRepository processingJobRepository;
    private final PDFFileRepository pdfFileRepository;

    private final SharedService sharedService;

    public ProcessingJob create(ProcessingJobDTO processingJobDto, User user, PDFFile pdfFile, ZonedDateTime createdAt) {

        ProcessingJob processingJobToCreate = processingJobRepository.save(ProcessingJob.builder()
                .pdfFile(pdfFile)
                .user(user)
                .prompt(processingJobDto.getPrompt())
                .status(ProcessingJobStatus.PENDING)
                .createdAt(createdAt)
                .build());

        log.info("ProcessingJob created successfully | user_id: {} | document_id: {}", user.getId(), pdfFile.getId());

        return processingJobToCreate;
    }

    @Transactional(readOnly = true)
    public ProcessingJobReadDTO findById(@NotNull Long processingJobId) {

        User currentUser = sharedService.getCurrentUser();

        return processingJobRepository.findAllByIdAndUserId(processingJobId, currentUser.getId())
                .map(processingJobReadMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Could not find ProcessingJob with id: " + processingJobId));
    }

    @Transactional(readOnly = true)
    public PageDTO<ProcessingJobReadDTO> findByAllByFileId(@NotNull Long fileId, Pageable pageable) {

        User currentUser = sharedService.getCurrentUser();

        if(pdfFileRepository.existsByIdAndUserId(fileId, currentUser.getId())) {
            Page<ProcessingJob> processingJobPage = processingJobRepository.findAllByPdfFile_Id(fileId, pageable);
            return processingJobReadMapper.toPageDto(processingJobPage);

        } else throw new EntityNotFoundException("File (ID: " + fileId + ") not found.");
    }

    public void maskAsDone(ProcessingJob processingJob, String resultS3Key, String responseMessage) {
        processingJob.setStatus(ProcessingJobStatus.SUCCESS);
        processingJob.setResultS3Key(resultS3Key);
        processingJob.setCompletedAt(ZonedDateTime.now());
        processingJob.setResponseMessage(responseMessage);
        processingJobRepository.save(processingJob);

        log.info("ProcessingJob SUCCESS status updated successfully | user_id: {} | document_id: {} | new status: {}",
                processingJob.getUser().getId(),
                processingJob.getPdfFile().getId(),
                ProcessingJobStatus.SUCCESS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(ProcessingJob processingJob, String responseMessage) {
        processingJob.setStatus(ProcessingJobStatus.FAILED);
        processingJob.setCompletedAt(ZonedDateTime.now());
        processingJob.setResultS3Key(responseMessage);
        processingJobRepository.save(processingJob);

        log.info("ProcessingJob FAILED status updated successfully | user_id: {} | document_id: {} | new status: {}",
                processingJob.getUser().getId(),
                processingJob.getPdfFile().getId(),
                ProcessingJobStatus.FAILED);

    }
}
