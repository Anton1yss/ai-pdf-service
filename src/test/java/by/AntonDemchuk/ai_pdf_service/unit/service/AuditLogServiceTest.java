package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogSearchParams;
import by.AntonDemchuk.ai_pdf_service.entity.*;
import by.AntonDemchuk.ai_pdf_service.mapper.auditLog.AuditLogReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.auditLog.AuditLogRepository;
import by.AntonDemchuk.ai_pdf_service.repository.auditLog.AuditLogSpecification;
import by.AntonDemchuk.ai_pdf_service.service.AuditLogService;
import by.AntonDemchuk.ai_pdf_service.service.SharedService;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceTest extends BaseServiceTest {

    @InjectMocks
    private AuditLogService auditLogService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogReadMapper auditLogReadMapper;

    @Mock
    private SharedService sharedService;

    @Test
    public void log_shouldSaveAuditLog_withUserOnly() {

        /* Acting */
        auditLogService.log(mockedUser, AuditLogAction.FILE_SAVE, "message", AuditLogStatus.COMPLETED);

        /* Asserting & Verifying */
        AuditLog saved = captureSavedLog();
        assertEquals(mockedUser, saved.getUser());
        assertEquals(AuditLogAction.FILE_SAVE, saved.getAction());
        assertEquals("message", saved.getMessage());
        assertEquals(AuditLogStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNull(saved.getPdfFile());
        assertNull(saved.getProcessingJob());
    }

    @Test
    public void log_shouldSaveAuditLog_withFile() {

        /* Acting */
        auditLogService.log(mockedUser, mockedPdfFile, AuditLogAction.FILE_SAVE, "message", AuditLogStatus.COMPLETED);

        /* Asserting & Verifying */
        AuditLog saved = captureSavedLog();
        assertEquals(mockedUser, saved.getUser());
        assertEquals(mockedPdfFile, saved.getPdfFile());
        assertEquals(AuditLogAction.FILE_SAVE, saved.getAction());
        assertEquals("message", saved.getMessage());
        assertEquals(AuditLogStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNull(saved.getProcessingJob());
    }

    @Test
    public void log_shouldSaveAuditLog_withFileAndProcessingJob() {

        /* Acting */
        auditLogService.log(mockedUser, mockedPdfFile, mockedProcessingJob,
                AuditLogAction.FILE_REDACTED, "failed", AuditLogStatus.FAILED);

        /* Asserting & Verifying */
        AuditLog saved = captureSavedLog();
        assertEquals(mockedUser, saved.getUser());
        assertEquals(mockedPdfFile, saved.getPdfFile());
        assertEquals(mockedProcessingJob, saved.getProcessingJob());
        assertEquals(AuditLogAction.FILE_REDACTED, saved.getAction());
        assertEquals("failed", saved.getMessage());
        assertEquals(AuditLogStatus.FAILED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAll_shouldReturnPageDto() {

        /* Arranging */
        AuditLogSearchParams params = mock(AuditLogSearchParams.class);
        Page<AuditLog> mockedPage = mock(Page.class);
        PageDTO<AuditLogReadDTO> expectedPageDto = mock(PageDTO.class);

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(mockedPage);
        when(auditLogReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        /* Acting */
        PageDTO<AuditLogReadDTO> res = auditLogService.findAll(params, pageable);

        /* Asserting & Verifying */
        assertEquals(expectedPageDto, res);
        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
        verify(auditLogReadMapper).toPageDto(mockedPage);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findMyLogs_shouldFilterByCurrentUser() {

        /* Arranging */
        ZonedDateTime from = ZonedDateTime.now().minusDays(7);
        ZonedDateTime to = ZonedDateTime.now();
        Specification<AuditLog> spec = mock(Specification.class);
        Page<AuditLog> mockedPage = mock(Page.class);
        PageDTO<AuditLogReadDTO> expectedPageDto = mock(PageDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(auditLogRepository.findAll(spec, pageable)).thenReturn(mockedPage);
        when(auditLogReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        try (MockedStatic<AuditLogSpecification> specification = mockStatic(AuditLogSpecification.class)) {
            specification.when(() -> AuditLogSpecification.buildSpecification(any(AuditLogSearchParams.class)))
                    .thenReturn(spec);

            /* Acting */
            PageDTO<AuditLogReadDTO> res = auditLogService.findMyLogs(
                    AuditLogStatus.COMPLETED, AuditLogAction.FILE_SAVE, from, to, pageable);

            /* Asserting & Verifying */
            assertEquals(expectedPageDto, res);

            ArgumentCaptor<AuditLogSearchParams> captor = ArgumentCaptor.forClass(AuditLogSearchParams.class);
            specification.verify(() -> AuditLogSpecification.buildSpecification(captor.capture()));
            AuditLogSearchParams params = captor.getValue();
            assertEquals(userId, params.getUserId());
            assertEquals(AuditLogStatus.COMPLETED, params.getStatus());
            assertEquals(AuditLogAction.FILE_SAVE, params.getAction());
            assertEquals(from, params.getFromDate());
            assertEquals(to, params.getToDate());
        }

        verify(auditLogRepository).findAll(spec, pageable);
        verify(auditLogReadMapper).toPageDto(mockedPage);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findByFile_shouldFilterByFileAndCurrentUser() {

        /* Arranging */
        Specification<AuditLog> spec = mock(Specification.class);
        Page<AuditLog> mockedPage = mock(Page.class);
        PageDTO<AuditLogReadDTO> expectedPageDto = mock(PageDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(auditLogRepository.findAll(spec, pageable)).thenReturn(mockedPage);
        when(auditLogReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        try (MockedStatic<AuditLogSpecification> specification = mockStatic(AuditLogSpecification.class)) {
            specification.when(() -> AuditLogSpecification.buildSpecification(any(AuditLogSearchParams.class)))
                    .thenReturn(spec);

            /* Acting */
            PageDTO<AuditLogReadDTO> res = auditLogService.findByFile(
                    fileId, AuditLogStatus.FAILED, AuditLogAction.FILE_REDACTED, pageable);

            /* Asserting & Verifying */
            assertEquals(expectedPageDto, res);

            ArgumentCaptor<AuditLogSearchParams> captor = ArgumentCaptor.forClass(AuditLogSearchParams.class);
            specification.verify(() -> AuditLogSpecification.buildSpecification(captor.capture()));
            AuditLogSearchParams params = captor.getValue();
            assertEquals(fileId, params.getPdfFileId());
            assertEquals(userId, params.getUserId());
            assertEquals(AuditLogStatus.FAILED, params.getStatus());
            assertEquals(AuditLogAction.FILE_REDACTED, params.getAction());
        }

        verify(auditLogRepository).findAll(spec, pageable);
        verify(auditLogReadMapper).toPageDto(mockedPage);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findByUser_shouldFilterByGivenUser_withoutCurrentUser() {

        /* Arranging */
        Long otherUserId = 2L;
        Specification<AuditLog> spec = mock(Specification.class);
        Page<AuditLog> mockedPage = mock(Page.class);
        PageDTO<AuditLogReadDTO> expectedPageDto = mock(PageDTO.class);

        when(auditLogRepository.findAll(spec, pageable)).thenReturn(mockedPage);
        when(auditLogReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        try (MockedStatic<AuditLogSpecification> specification = mockStatic(AuditLogSpecification.class)) {
            specification.when(() -> AuditLogSpecification.buildSpecification(any(AuditLogSearchParams.class)))
                    .thenReturn(spec);

            /* Acting */
            PageDTO<AuditLogReadDTO> res = auditLogService.findByUser(
                    otherUserId, AuditLogStatus.COMPLETED, AuditLogAction.FILE_SAVE, pageable);

            /* Asserting & Verifying */
            assertEquals(expectedPageDto, res);

            ArgumentCaptor<AuditLogSearchParams> captor = ArgumentCaptor.forClass(AuditLogSearchParams.class);
            specification.verify(() -> AuditLogSpecification.buildSpecification(captor.capture()));
            AuditLogSearchParams params = captor.getValue();
            assertEquals(otherUserId, params.getUserId());
            assertEquals(AuditLogStatus.COMPLETED, params.getStatus());
            assertEquals(AuditLogAction.FILE_SAVE, params.getAction());
        }

        verify(auditLogRepository).findAll(spec, pageable);
        verifyNoInteractions(sharedService);
    }

    private AuditLog captureSavedLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}