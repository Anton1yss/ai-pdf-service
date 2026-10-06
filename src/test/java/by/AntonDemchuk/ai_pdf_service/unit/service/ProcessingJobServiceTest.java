package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.*;
import by.AntonDemchuk.ai_pdf_service.mapper.processingJob.ProcessingJobMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.processingJob.ProcessingJobReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.PDFFileRepository;
import by.AntonDemchuk.ai_pdf_service.repository.ProcessingJobRepository;
import by.AntonDemchuk.ai_pdf_service.service.ProcessingJobService;
import by.AntonDemchuk.ai_pdf_service.service.SharedService;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessingJobServiceTest extends BaseServiceTest {

    private final Long processingJobId = 5L;

    @InjectMocks
    private ProcessingJobService processingJobService;

    @Mock
    private ProcessingJobMapper processingJobMapper;

    @Mock
    private ProcessingJobReadMapper processingJobReadMapper;

    @Mock
    private ProcessingJobRepository processingJobRepository;

    @Mock
    private PDFFileRepository pdfFileRepository;

    @Mock
    private SharedService sharedService;

    @Test
    public void create_shouldCreatePendingProcessingJob() {

        /* Arranging */
        ZonedDateTime createdAt = ZonedDateTime.now();

        when(processingJobRepository.save(any(ProcessingJob.class))).thenReturn(mockedProcessingJob);

        /* Acting */
        ProcessingJob res = processingJobService.create(
                ProcessingJobAction.FILE_REDACTION, mockedUser, mockedPdfFile, createdAt);

        /* Asserting & Verifying */
        assertEquals(mockedProcessingJob, res);

        ArgumentCaptor<ProcessingJob> captor = ArgumentCaptor.forClass(ProcessingJob.class);
        verify(processingJobRepository).save(captor.capture());
        ProcessingJob saved = captor.getValue();
        assertEquals(ProcessingJobAction.FILE_REDACTION, saved.getAction());
        assertEquals(ProcessingJobStatus.PENDING, saved.getStatus());
        assertEquals(mockedUser, saved.getUser());
        assertEquals(mockedPdfFile, saved.getPdfFile());
        assertEquals(createdAt, saved.getCreatedAt());
    }

    @Test
    public void findById_shouldFindProcessingJob() {

        /* Arranging */
        ProcessingJobReadDTO expectedDto = mock(ProcessingJobReadDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(processingJobRepository.findAllByIdAndUserId(processingJobId, userId))
                .thenReturn(Optional.of(mockedProcessingJob));
        when(processingJobReadMapper.toDto(mockedProcessingJob)).thenReturn(expectedDto);

        /* Acting */
        ProcessingJobReadDTO res = processingJobService.findById(processingJobId);

        /* Asserting & Verifying */
        assertEquals(expectedDto, res);
        verify(processingJobRepository).findAllByIdAndUserId(processingJobId, userId);
        verify(processingJobReadMapper).toDto(mockedProcessingJob);
    }

    @Test
    public void findById_shouldThrow_whenProcessingJobNotFound() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(processingJobRepository.findAllByIdAndUserId(processingJobId, userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> processingJobService.findById(processingJobId));

        /* Verifying */
        verifyNoInteractions(processingJobReadMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findByAllByFileId_shouldReturnPageDto() {

        /* Arranging */
        Page<ProcessingJob> mockedPage = mock(Page.class);
        PageDTO<ProcessingJobReadDTO> expectedPageDto = mock(PageDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.existsByIdAndUserId(fileId, userId)).thenReturn(true);
        when(processingJobRepository.findAllByPdfFile_Id(fileId, pageable)).thenReturn(mockedPage);
        when(processingJobReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        /* Acting */
        PageDTO<ProcessingJobReadDTO> res = processingJobService.findByAllByFileId(fileId, pageable);

        /* Asserting & Verifying */
        assertEquals(expectedPageDto, res);
        verify(pdfFileRepository).existsByIdAndUserId(fileId, userId);
        verify(processingJobRepository).findAllByPdfFile_Id(fileId, pageable);
        verify(processingJobReadMapper).toPageDto(mockedPage);
    }

    @Test
    public void findByAllByFileId_shouldThrow_whenFileDoesNotBelongToUser() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.existsByIdAndUserId(fileId, userId)).thenReturn(false);

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> processingJobService.findByAllByFileId(fileId, pageable));

        /* Verifying */
        verifyNoInteractions(processingJobRepository, processingJobReadMapper);
    }

    @Test
    public void maskAsDone_shouldMarkJobAsSuccess() {

        /* Arranging */
        ProcessingJob job = pendingJob();

        /* Acting */
        processingJobService.maskAsDone(job, "processed/result.pdf", "done");

        /* Asserting & Verifying */
        assertEquals(ProcessingJobStatus.SUCCESS, job.getStatus());
        assertEquals("processed/result.pdf", job.getResultS3Key());
        assertEquals("done", job.getResponseMessage());
        assertNotNull(job.getCompletedAt());
        verify(processingJobRepository).save(job);
    }

    @Test
    public void markAsFailed_shouldMarkJobAsFailed() {

        /* Arranging */
        ProcessingJob job = pendingJob();

        /* Acting */
        processingJobService.markAsFailed(job, "boom");

        /* Asserting & Verifying */
        assertEquals(ProcessingJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        verify(processingJobRepository).save(job);
    }

    private ProcessingJob pendingJob() {
        return ProcessingJob.builder()
                .user(mockedUser)
                .pdfFile(mockedPdfFile)
                .action(ProcessingJobAction.FILE_REDACTION)
                .status(ProcessingJobStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();
    }
}