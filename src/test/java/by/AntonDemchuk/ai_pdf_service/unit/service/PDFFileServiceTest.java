package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.*;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.*;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileEncryptionSettingsMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileReadMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.PDFFileRepository;
import by.AntonDemchuk.ai_pdf_service.service.*;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.exceptions.BadPasswordException;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PDFFileServiceTest extends BaseServiceTest {

    private static final String PRESIGNED_URL = "https://s3.example.com/presigned";
    private static final String FILE_NAME = "document.pdf";
    private static final String S3_KEY = "originals/document.pdf";
    private static final String NEW_S3_KEY = "processed/document.pdf";

    @InjectMocks
    private PDFFileService pdfFileService;

    @Mock
    private PDFFileRepository pdfFileRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private SharedService sharedService;

    @Mock
    private AIService aiService;

    @Mock
    private ProcessingJobService processingJobService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PDFFileMapper pdfDocumentMapper;

    @Mock
    private PDFFileEncryptionSettingsMapper pdfEncryptionSettingsMapper;

    @Mock
    private PDFFileReadMapper pdfDocumentReadMapper;

    @Mock
    private UserReadMapper userReadMapper;

    @Test
    public void create_shouldCreatePdfFile() throws IOException {

        /* Arranging */
        MockMultipartFile document = new MockMultipartFile(
                "file", FILE_NAME, "application/pdf", createPdf("Hello"));
        PDFFIleDTO expectedDto = mock(PDFFIleDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(s3Service.uploadDocument(document, mockedUser, "originals")).thenReturn(S3_KEY);
        when(pdfFileRepository.save(any(PDFFile.class))).thenReturn(mockedPdfFile);
        when(pdfDocumentMapper.toDto(mockedPdfFile)).thenReturn(expectedDto);

        /* Acting */
        PDFFIleDTO res = pdfFileService.create(document);

        /* Asserting & Verifying */
        assertEquals(expectedDto, res);

        ArgumentCaptor<PDFFile> captor = ArgumentCaptor.forClass(PDFFile.class);
        verify(pdfFileRepository).save(captor.capture());
        PDFFile saved = captor.getValue();
        assertEquals(FILE_NAME, saved.getName());
        assertEquals(S3_KEY, saved.getS3Key());
        assertEquals(S3_KEY, saved.getOriginalS3Key());
        assertEquals(1L, saved.getVersion());
        assertEquals(mockedUser, saved.getUser());

        verify(s3Service).uploadDocument(document, mockedUser, "originals");
        verify(auditLogService).log(eq(mockedUser), eq(mockedPdfFile), eq(AuditLogAction.FILE_SAVE),
                anyString(), eq(AuditLogStatus.COMPLETED));
        verify(pdfDocumentMapper).toDto(mockedPdfFile);
    }

    @Test
    public void create_shouldThrow_whenFileIsNull() {

        /* Acting & Asserting */
        assertThrows(IllegalArgumentException.class, () -> pdfFileService.create(null));

        /* Verifying */
        verifyNoInteractions(s3Service, pdfFileRepository, auditLogService);
    }

    @Test
    public void create_shouldThrow_whenFileIsEmpty() {

        /* Arranging */
        MockMultipartFile empty = new MockMultipartFile("file", FILE_NAME, "application/pdf", new byte[0]);

        /* Acting & Asserting */
        assertThrows(IllegalArgumentException.class, () -> pdfFileService.create(empty));

        /* Verifying */
        verifyNoInteractions(s3Service, pdfFileRepository, auditLogService);
    }

    @Test
    public void create_shouldThrow_whenFileIsTooLarge() {

        /* Arranging */
        MultipartFile bigFile = mock(MultipartFile.class);
        when(bigFile.isEmpty()).thenReturn(false);
        when(bigFile.getSize()).thenReturn(PDFFileValidation.MAX_FILE_SIZE + 1);

        /* Acting & Asserting */
        assertThrows(IllegalArgumentException.class, () -> pdfFileService.create(bigFile));

        /* Verifying */
        verifyNoInteractions(s3Service, pdfFileRepository, auditLogService);
    }

    @Test
    public void create_shouldThrow_whenContentTypeIsNotPdf() {

        /* Arranging */
        MockMultipartFile textFile = new MockMultipartFile("file", FILE_NAME, "text/plain", "text".getBytes());

        /* Acting & Asserting */
        assertThrows(IllegalArgumentException.class, () -> pdfFileService.create(textFile));

        /* Verifying */
        verifyNoInteractions(s3Service, pdfFileRepository, auditLogService);
    }

    @Test
    public void create_shouldThrow_whenExtensionIsNotPdf() {

        /* Arranging */
        MockMultipartFile wrongExtension = new MockMultipartFile("file", "document.txt", "application/pdf", "text".getBytes());

        /* Acting & Asserting */
        assertThrows(IllegalArgumentException.class, () -> pdfFileService.create(wrongExtension));

        /* Verifying */
        verifyNoInteractions(s3Service, pdfFileRepository, auditLogService);
    }

    @Test
    public void redact_shouldRedactPhrases() throws IOException {

        /* Arranging */
        PDFFile redactedFile = mock(PDFFile.class);
        Long redactedFileId = 11L;
        PDFFileRedactDTO redactDto = mock(PDFFileRedactDTO.class);
        UserReadDTO userReadDto = mock(UserReadDTO.class);
        byte[] original = createPdf("My secret password is hidden");

        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_REDACTION);

        when(redactDto.getPrompt()).thenReturn("redact secrets");
        when(s3Service.downloadDocument(S3_KEY)).thenReturn(original);
        when(aiService.getPhrasesToRedact(anyString(), eq("redact secrets"))).thenReturn(List.of("secret"));
        when(s3Service.uploadDocument(any(byte[].class), eq(FILE_NAME), eq(mockedUser), eq("processed")))
                .thenReturn(NEW_S3_KEY);
        when(pdfFileRepository.save(any(PDFFile.class))).thenReturn(redactedFile);

        /* findPDFDocumentById */
        when(redactedFile.getId()).thenReturn(redactedFileId);
        when(redactedFile.getName()).thenReturn(FILE_NAME);
        when(redactedFile.getS3Key()).thenReturn(NEW_S3_KEY);
        when(redactedFile.getVersion()).thenReturn(2L);
        when(pdfFileRepository.findByIdAndUserId(redactedFileId, userId)).thenReturn(Optional.of(redactedFile));
        when(s3Service.generatePreSignedURL(NEW_S3_KEY)).thenReturn(PRESIGNED_URL);
        when(userReadMapper.toDto(mockedUser)).thenReturn(userReadDto);

        /* Acting */
        PDFFileDetailedReadDTO res = pdfFileService.redact(fileId, redactDto);

        /* Asserting & Verifying */
        assertEquals(redactedFileId, res.getId());
        assertEquals(FILE_NAME, res.getName());
        assertEquals(PRESIGNED_URL, res.getPreSignedURL());
        assertEquals(2L, res.getVersion());
        assertEquals(userReadDto, res.getUser());

        ArgumentCaptor<byte[]> uploaded = ArgumentCaptor.forClass(byte[].class);
        verify(s3Service).uploadDocument(uploaded.capture(), eq(FILE_NAME), eq(mockedUser), eq("processed"));
        String redactedText = extractText(uploaded.getValue());
        assertFalse(redactedText.contains("secret"), "Phrase must be removed from the redacted file");
        assertTrue(redactedText.contains("password"));

        ArgumentCaptor<PDFFile> saved = ArgumentCaptor.forClass(PDFFile.class);
        verify(pdfFileRepository).save(saved.capture());
        assertEquals(mockedPdfFile, saved.getValue().getParentFile());
        assertEquals(NEW_S3_KEY, saved.getValue().getS3Key());
        assertEquals(S3_KEY, saved.getValue().getOriginalS3Key());
        assertEquals(2L, saved.getValue().getVersion());

        verify(processingJobService).maskAsDone(eq(mockedProcessingJob), eq(NEW_S3_KEY), anyString());
        verify(auditLogService).log(eq(mockedUser), eq(redactedFile), eq(mockedProcessingJob),
                eq(AuditLogAction.FILE_REDACTED), anyString(), eq(AuditLogStatus.COMPLETED));
        verify(processingJobService, never()).markAsFailed(any(), anyString());
    }

    @Test
    public void redact_shouldLeaveFileUntouched_whenNoPhraseMatches() throws IOException {

        /* Arranging */
        PDFFile redactedFile = mock(PDFFile.class);
        Long redactedFileId = 11L;
        PDFFileRedactDTO redactDto = mock(PDFFileRedactDTO.class);

        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_REDACTION);

        when(redactDto.getPrompt()).thenReturn("redact");
        when(s3Service.downloadDocument(S3_KEY)).thenReturn(createPdf("Nothing to hide"));
        when(aiService.getPhrasesToRedact(anyString(), eq("redact"))).thenReturn(List.of("absent-phrase"));
        when(s3Service.uploadDocument(any(byte[].class), eq(FILE_NAME), eq(mockedUser), eq("processed")))
                .thenReturn(NEW_S3_KEY);
        when(pdfFileRepository.save(any(PDFFile.class))).thenReturn(redactedFile);
        when(redactedFile.getId()).thenReturn(redactedFileId);
        when(pdfFileRepository.findByIdAndUserId(redactedFileId, userId)).thenReturn(Optional.of(redactedFile));

        /* Acting */
        pdfFileService.redact(fileId, redactDto);

        /* Asserting & Verifying */
        ArgumentCaptor<byte[]> uploaded = ArgumentCaptor.forClass(byte[].class);
        verify(s3Service).uploadDocument(uploaded.capture(), eq(FILE_NAME), eq(mockedUser), eq("processed"));
        assertTrue(extractText(uploaded.getValue()).contains("Nothing to hide"));
    }

    @Test
    public void redact_shouldThrow_whenFileNotFound() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> pdfFileService.redact(fileId, mock(PDFFileRedactDTO.class)));

        /* Verifying */
        verifyNoInteractions(processingJobService, s3Service, aiService);
    }

    @Test
    public void redact_shouldMarkJobAsFailed_whenProcessingFails() {

        /* Arranging */
        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_REDACTION);
        when(s3Service.downloadDocument(S3_KEY)).thenThrow(new RuntimeException("boom"));

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfFileService.redact(fileId, mock(PDFFileRedactDTO.class)));
        assertEquals("Failed to redact pdfFile", ex.getMessage());

        /* Verifying */
        verify(processingJobService).markAsFailed(mockedProcessingJob, "boom");
        verify(auditLogService).log(mockedUser, mockedPdfFile, mockedProcessingJob,
                AuditLogAction.FILE_REDACTED, "boom", AuditLogStatus.FAILED);
        verify(pdfFileRepository, never()).save(any());
    }

    @Test
    public void encrypt_shouldEncryptFile() throws IOException {

        /* Arranging */
        PDFFile encryptedFile = mock(PDFFile.class);
        Long encryptedFileId = 12L;
        PDFEncryptionSettingsDTO encryptionDto = mock(PDFEncryptionSettingsDTO.class);
        PDFEncryptionSettings encryptionSettings = mock(PDFEncryptionSettings.class);

        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_ENCRYPTION);

        when(encryptionDto.getUserPass()).thenReturn("user-pass");
        when(encryptionDto.getOwnerPass()).thenReturn("owner-pass");
        when(s3Service.downloadDocument(S3_KEY)).thenReturn(createPdf("Top secret"));
        when(s3Service.uploadDocument(any(byte[].class), eq(FILE_NAME), eq(mockedUser), eq("processed")))
                .thenReturn(NEW_S3_KEY);
        when(pdfEncryptionSettingsMapper.toEntity(encryptionDto)).thenReturn(encryptionSettings);
        when(pdfFileRepository.save(any(PDFFile.class))).thenReturn(encryptedFile);
        when(encryptedFile.getId()).thenReturn(encryptedFileId);
        when(pdfFileRepository.findByIdAndUserId(encryptedFileId, userId)).thenReturn(Optional.of(encryptedFile));

        /* Acting */
        PDFFileDetailedReadDTO res = pdfFileService.encrypt(fileId, encryptionDto);

        /* Asserting & Verifying */
        assertEquals(encryptedFileId, res.getId());

        ArgumentCaptor<byte[]> uploaded = ArgumentCaptor.forClass(byte[].class);
        verify(s3Service).uploadDocument(uploaded.capture(), eq(FILE_NAME), eq(mockedUser), eq("processed"));
        byte[] encryptedBytes = uploaded.getValue();

        /* can't be opened without password, can be opened with the user or the owner password */
        assertThrows(BadPasswordException.class, () -> openPdf(encryptedBytes, null));
        assertDoesNotThrow(() -> openPdf(encryptedBytes, "user-pass"));
        assertDoesNotThrow(() -> openPdf(encryptedBytes, "owner-pass"));

        ArgumentCaptor<PDFFile> saved = ArgumentCaptor.forClass(PDFFile.class);
        verify(pdfFileRepository).save(saved.capture());
        assertEquals(encryptionSettings, saved.getValue().getEncryptionSettings());
        assertEquals(mockedPdfFile, saved.getValue().getParentFile());
        assertEquals(2L, saved.getValue().getVersion());

        verify(processingJobService).maskAsDone(eq(mockedProcessingJob), eq(NEW_S3_KEY), anyString());
        verify(auditLogService).log(eq(mockedUser), eq(encryptedFile), eq(mockedProcessingJob),
                any(AuditLogAction.class), anyString(), eq(AuditLogStatus.COMPLETED));
    }

    @Test
    public void encrypt_shouldThrow_whenFileNotFound() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class,
                () -> pdfFileService.encrypt(fileId, mock(PDFEncryptionSettingsDTO.class)));

        /* Verifying */
        verifyNoInteractions(processingJobService, s3Service);
    }

    @Test
    public void encrypt_shouldMarkJobAsFailed_whenProcessingFails() {

        /* Arranging */
        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_ENCRYPTION);
        when(s3Service.downloadDocument(S3_KEY)).thenThrow(new RuntimeException("boom"));

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfFileService.encrypt(fileId, mock(PDFEncryptionSettingsDTO.class)));
        assertEquals("Failed to encrypt pdfFile", ex.getMessage());

        /* Verifying */
        verify(processingJobService).markAsFailed(mockedProcessingJob, "boom");
        verify(auditLogService).log(eq(mockedUser), eq(mockedPdfFile), eq(mockedProcessingJob),
                any(AuditLogAction.class), eq("boom"), eq(AuditLogStatus.FAILED));
        verify(pdfFileRepository, never()).save(any());
    }

    @Test
    public void decrypt_shouldDecryptFile() throws IOException {

        /* Arranging */
        PDFFile decryptedFile = mock(PDFFile.class);
        Long decryptedFileId = 13L;

        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_DECRYPTION);

        when(s3Service.downloadDocument(S3_KEY)).thenReturn(createEncryptedPdf("Top secret", "user-pass", "owner-pass"));
        when(s3Service.uploadDocument(any(byte[].class), eq(FILE_NAME), eq(mockedUser), eq("processed")))
                .thenReturn(NEW_S3_KEY);
        when(pdfFileRepository.save(any(PDFFile.class))).thenReturn(decryptedFile);
        when(decryptedFile.getId()).thenReturn(decryptedFileId);
        when(pdfFileRepository.findByIdAndUserId(decryptedFileId, userId)).thenReturn(Optional.of(decryptedFile));

        /* Acting */
        PDFFileDetailedReadDTO res = pdfFileService.decrypt(fileId, "owner-pass");

        /* Asserting & Verifying */
        assertEquals(decryptedFileId, res.getId());

        ArgumentCaptor<byte[]> uploaded = ArgumentCaptor.forClass(byte[].class);
        verify(s3Service).uploadDocument(uploaded.capture(), eq(FILE_NAME), eq(mockedUser), eq("processed"));
        assertEquals("Top secret", extractText(uploaded.getValue()),
                "Decrypted file must be readable without a password");

        ArgumentCaptor<PDFFile> saved = ArgumentCaptor.forClass(PDFFile.class);
        verify(pdfFileRepository).save(saved.capture());
        assertNotNull(saved.getValue().getEncryptionSettings());
        assertEquals(mockedPdfFile, saved.getValue().getParentFile());
        assertEquals(2L, saved.getValue().getVersion());

        verify(processingJobService).maskAsDone(eq(mockedProcessingJob), eq(NEW_S3_KEY), anyString());
        verify(auditLogService).log(eq(mockedUser), eq(decryptedFile), eq(mockedProcessingJob),
                eq(AuditLogAction.FILE_DECRYPT), anyString(), eq(AuditLogStatus.COMPLETED));
    }

    @Test
    public void decrypt_shouldMarkJobAsFailed_whenPasswordIsWrong() throws IOException {

        /* Arranging */
        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_DECRYPTION);
        when(s3Service.downloadDocument(S3_KEY)).thenReturn(createEncryptedPdf("Top secret", "user-pass", "owner-pass"));

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfFileService.decrypt(fileId, "wrong-pass"));
        assertEquals("Failed to re-encrypt pdfFile", ex.getMessage());

        /* Verifying */
        verify(processingJobService).markAsFailed(eq(mockedProcessingJob), anyString());
        verify(auditLogService).log(eq(mockedUser), eq(mockedPdfFile), eq(mockedProcessingJob),
                eq(AuditLogAction.FILE_DECRYPT), anyString(), eq(AuditLogStatus.FAILED));
        verify(s3Service, never()).uploadDocument(any(byte[].class), anyString(), any(User.class), anyString());
        verify(pdfFileRepository, never()).save(any());
    }

    @Test
    public void decrypt_shouldThrow_whenFileNotFound() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> pdfFileService.decrypt(fileId, "owner-pass"));

        /* Verifying */
        verifyNoInteractions(processingJobService, s3Service);
    }

    @Test
    public void summarize_shouldReturnSummary() throws IOException {

        /* Arranging */
        PDFFileSummarizeDTO summarizeDto = mock(PDFFileSummarizeDTO.class);

        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_SUMMARIZE);

        when(mockedPdfFile.getId()).thenReturn(fileId);
        when(summarizeDto.getPrompt()).thenReturn("summarize");
        when(s3Service.downloadDocument(S3_KEY)).thenReturn(createPdf("Some long text"));
        when(aiService.summarize(anyString(), eq("summarize"))).thenReturn("Short summary");

        /* Acting */
        PDFFileSummarizeResponseDTO res = pdfFileService.summarize(fileId, summarizeDto);

        /* Asserting & Verifying */
        assertEquals(fileId, res.getFileId());
        assertEquals("Short summary", res.getResponse());

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).summarize(textCaptor.capture(), eq("summarize"));
        assertTrue(textCaptor.getValue().contains("Some long text"));

        verify(processingJobService).maskAsDone(mockedProcessingJob, S3_KEY, "Short summary");
        verify(auditLogService).log(eq(mockedUser), eq(mockedPdfFile), eq(mockedProcessingJob),
                eq(AuditLogAction.FILE_SUMMARIZE), anyString(), eq(AuditLogStatus.COMPLETED));
        verify(pdfFileRepository, never()).save(any());
    }

    @Test
    public void summarize_shouldThrow_whenFileNotFound() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class,
                () -> pdfFileService.summarize(fileId, mock(PDFFileSummarizeDTO.class)));

        /* Verifying */
        verifyNoInteractions(processingJobService, s3Service, aiService);
    }

    @Test
    public void summarize_shouldMarkJobAsFailed_whenAiFails() throws IOException {

        /* Arranging */
        PDFFileSummarizeDTO summarizeDto = mock(PDFFileSummarizeDTO.class);

        mockCurrentUserAndFile();
        mockProcessingJob(ProcessingJobAction.FILE_SUMMARIZE);

        when(summarizeDto.getPrompt()).thenReturn("summarize");
        when(s3Service.downloadDocument(S3_KEY)).thenReturn(createPdf("Some long text"));
        when(aiService.summarize(anyString(), eq("summarize"))).thenThrow(new RuntimeException("ai down"));

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfFileService.summarize(fileId, summarizeDto));
        assertEquals("Failed to summarize pdfFile", ex.getMessage());

        /* Verifying */
        verify(processingJobService).markAsFailed(mockedProcessingJob, "ai down");
        verify(auditLogService).log(mockedUser, mockedPdfFile, mockedProcessingJob,
                AuditLogAction.FILE_SUMMARIZE, "ai down", AuditLogStatus.FAILED);
        verify(processingJobService, never()).maskAsDone(any(), anyString(), anyString());
    }

    @Test
    public void findPDFDocumentById_shouldFindFile() {

        /* Arranging */
        UserReadDTO userReadDto = mock(UserReadDTO.class);
        PDFEncryptionSettings encryptionSettings = mock(PDFEncryptionSettings.class);
        ZonedDateTime createdAt = ZonedDateTime.now().minusDays(1);
        ZonedDateTime updatedAt = ZonedDateTime.now();

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.of(mockedPdfFile));
        when(mockedPdfFile.getId()).thenReturn(fileId);
        when(mockedPdfFile.getName()).thenReturn(FILE_NAME);
        when(mockedPdfFile.getS3Key()).thenReturn(S3_KEY);
        when(mockedPdfFile.getVersion()).thenReturn(1L);
        when(mockedPdfFile.getCreatedAt()).thenReturn(createdAt);
        when(mockedPdfFile.getUpdatedAt()).thenReturn(updatedAt);
        when(mockedPdfFile.getEncryptionSettings()).thenReturn(encryptionSettings);
        when(s3Service.generatePreSignedURL(S3_KEY)).thenReturn(PRESIGNED_URL);
        when(userReadMapper.toDto(mockedUser)).thenReturn(userReadDto);

        /* Acting */
        PDFFileDetailedReadDTO res = pdfFileService.findPDFDocumentById(fileId);

        /* Asserting & Verifying */
        assertEquals(fileId, res.getId());
        assertEquals(FILE_NAME, res.getName());
        assertEquals(PRESIGNED_URL, res.getPreSignedURL());
        assertEquals(1L, res.getVersion());
        assertEquals(createdAt, res.getCreatedAt());
        assertEquals(updatedAt, res.getUpdatedAt());
        assertEquals(userReadDto, res.getUser());
        assertEquals(encryptionSettings, res.getEncryptionSettings());
        verify(pdfFileRepository).findByIdAndUserId(fileId, userId);
        verify(s3Service).generatePreSignedURL(S3_KEY);
    }

    @Test
    public void findPDFDocumentById_shouldThrow_whenFileNotFound() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> pdfFileService.findPDFDocumentById(fileId));

        /* Verifying */
        verifyNoInteractions(s3Service, userReadMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllPDFFiles_shouldReturnPageDto() {

        /* Arranging */
        Page<PDFFile> mockedPage = mock(Page.class);
        PageDTO<PDFFileReadDTO> expectedPageDto = mock(PageDTO.class);

        when(pdfFileRepository.findAll(pageable)).thenReturn(mockedPage);
        when(pdfDocumentReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        /* Acting */
        PageDTO<PDFFileReadDTO> res = pdfFileService.findAllPDFFiles(pageable);

        /* Asserting & Verifying */
        assertEquals(expectedPageDto, res);
        verify(pdfFileRepository).findAll(pageable);
        verify(pdfDocumentReadMapper).toPageDto(mockedPage);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAllPDFFilesByUserId_shouldReturnPageDto() {

        /* Arranging */
        Page<PDFFile> mockedPage = mock(Page.class);
        PageDTO<PDFFileReadDTO> expectedPageDto = mock(PageDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findAllByUserId(userId, pageable)).thenReturn(mockedPage);
        when(pdfDocumentReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        /* Acting */
        PageDTO<PDFFileReadDTO> res = pdfFileService.findAllPDFFilesByUserId(pageable);

        /* Asserting & Verifying */
        assertEquals(expectedPageDto, res);
        verify(pdfFileRepository).findAllByUserId(userId, pageable);
        verify(pdfDocumentReadMapper).toPageDto(mockedPage);
    }

    @Test
    public void deletePDFDocument_shouldDeleteFile() throws IOException {

        /* Arranging */
        when(pdfFileRepository.findById(fileId)).thenReturn(Optional.of(mockedPdfFile));
        when(mockedPdfFile.getS3Key()).thenReturn(S3_KEY);
        when(mockedPdfFile.getId()).thenReturn(fileId);

        /* Acting */
        pdfFileService.deletePDFDocument(fileId);

        /* Verifying */
        verify(pdfFileRepository).findById(fileId);
        verify(s3Service).deleteDocument(S3_KEY);
        verify(pdfFileRepository).deleteById(fileId);
    }

    @Test
    public void deletePDFDocument_shouldThrow_whenFileNotFound() {

        /* Arranging */
        when(pdfFileRepository.findById(fileId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> pdfFileService.deletePDFDocument(fileId));

        /* Verifying */
        verifyNoInteractions(s3Service);
        verify(pdfFileRepository, never()).deleteById(any());
    }

    @Test
    public void deletePDFDocument_shouldThrowIOException_whenS3Fails() throws IOException {

        /* Arranging */
        when(pdfFileRepository.findById(fileId)).thenReturn(Optional.of(mockedPdfFile));
        when(mockedPdfFile.getS3Key()).thenReturn(S3_KEY);
        doThrow(new RuntimeException("s3 down")).when(s3Service).deleteDocument(S3_KEY);

        /* Acting & Asserting */
        assertThrows(IOException.class, () -> pdfFileService.deletePDFDocument(fileId));

        /* Verifying */
        verify(pdfFileRepository, never()).deleteById(any());
    }

    private void mockCurrentUserAndFile() {
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(pdfFileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.of(mockedPdfFile));
        when(mockedPdfFile.getS3Key()).thenReturn(S3_KEY);
        lenient().when(mockedPdfFile.getName()).thenReturn(FILE_NAME);
        lenient().when(mockedPdfFile.getOriginalS3Key()).thenReturn(S3_KEY);
        lenient().when(mockedPdfFile.getVersion()).thenReturn(1L);
    }

    private void mockProcessingJob(ProcessingJobAction action) {
        when(processingJobService.create(eq(action), eq(mockedUser), eq(mockedPdfFile), any(ZonedDateTime.class)))
                .thenReturn(mockedProcessingJob);
    }

    private byte[] createPdf(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTextPage(new PdfWriter(out), text);
        return out.toByteArray();
    }

    private byte[] createEncryptedPdf(String text, String userPass, String ownerPass) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WriterProperties properties = new WriterProperties().setStandardEncryption(
                userPass.getBytes(),
                ownerPass.getBytes(),
                EncryptionConstants.ALLOW_PRINTING,
                EncryptionConstants.ENCRYPTION_AES_256);
        writeTextPage(new PdfWriter(out, properties), text);
        return out.toByteArray();
    }

    private void writeTextPage(PdfWriter writer, String text) throws IOException {
        try (PdfDocument document = new PdfDocument(writer)) {
            PdfCanvas canvas = new PdfCanvas(document.addNewPage());
            canvas.beginText()
                    .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 12)
                    .moveText(50, 700)
                    .showText(text)
                    .endText();
        }
    }

    private String extractText(byte[] pdfBytes) throws IOException {
        try (PdfDocument document = openPdf(pdfBytes, null)) {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(page)));
            }
            return text.toString();
        }
    }

    private PdfDocument openPdf(byte[] pdfBytes, String password) throws IOException {
        ReaderProperties properties = new ReaderProperties();
        if (password != null) {
            properties.setPassword(password.getBytes());
        }
        return new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes), properties));
    }
}