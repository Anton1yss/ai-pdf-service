package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.*;

import by.AntonDemchuk.ai_pdf_service.entity.*;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileEncryptionSettingsMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileReadMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.PDFFileRepository;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfDocumentContentParser;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;
import com.itextpdf.pdfcleanup.CleanUpProperties;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;
import com.itextpdf.pdfcleanup.PdfCleanUpTool;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@Validated
public class PDFFileService {

    private final PDFFileRepository pdfFileRepository;

    private final S3Service s3Service;
    private final SharedService sharedService;
    private final AIService aiService;
    private final ProcessingJobService processingJobService;
    private final AuditLogService auditLogService;

    private final PDFFileMapper pdfDocumentMapper;
    private final PDFFileEncryptionSettingsMapper pdfEncryptionSettingsMapper;
    private final PDFFileReadMapper pdfDocumentReadMapper;
    private final UserReadMapper userReadMapper;

    public PDFFIleDTO create(MultipartFile document) throws IOException {

        validateDocument(document);

        User currentUser = sharedService.getCurrentUser();

        String key = s3Service.uploadDocument(document, currentUser, "originals");

        PDFFile fileToCreate = pdfFileRepository.save(PDFFile.builder()
                .name(document.getOriginalFilename())
                .originalS3Key(key)
                .s3Key(key)
                .version(1L)
                .user(currentUser)
                .encryptionSettings(new PDFEncryptionSettings())
                .build());

        log.info("Document created successfully | user: {} | document: {}", currentUser.getUsername(), document.getName());
        auditLogService.log(currentUser, fileToCreate, AuditLogAction.FILE_SAVE, "PDF File successfully created", AuditLogStatus.COMPLETED);

        return pdfDocumentMapper.toDto(fileToCreate);
    }

    public PDFFileDetailedReadDTO redactContent(@NotNull Long fileId, PDFFileRedactDTO PDFFileRedactDto) {

        User currentUser = sharedService.getCurrentUser();

        PDFFile pdfFile = pdfFileRepository.findByIdAndUserId(fileId, currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Document not found | user: {} | pdfFile: {}", currentUser.getUsername(), fileId);
                    return new EntityNotFoundException("Document not found | user: " + currentUser.getUsername());
                });

        ProcessingJob processingJob = processingJobService.create(
                ProcessingJobAction.FILE_REDACT,
                currentUser,
                pdfFile,
                ZonedDateTime.now());

        PDFFile redactedFile = null;

        try {
            byte[] fileToRedact = s3Service.downloadDocument(pdfFile.getS3Key());

            String fileText = extractDocumentText(fileToRedact);

            List<String> phrasesToRedact = aiService.getPhrasesToRedact(fileText, PDFFileRedactDto.getPrompt());

            String key = s3Service.uploadDocument(
                    redactPhrases(fileToRedact, phrasesToRedact),
                    pdfFile.getName(),
                    currentUser,
                    "processed");

            redactedFile = pdfFileRepository.save(PDFFile.builder()
                    .name(pdfFile.getName())
                    .originalS3Key(pdfFile.getOriginalS3Key())
                    .s3Key(key)
                    .parentFile(pdfFile)
                    .version(pdfFile.getVersion() + 1L)
                    .user(currentUser)
                    .encryptionSettings(pdfFile.getEncryptionSettings())
                    .build());

            processingJobService.maskAsDone(processingJob, key, phrasesToRedact.toString());

            log.info("Document redacted successfully | user_id: {} | file_id: {}", currentUser.getId(), pdfFile.getId());
            auditLogService.log(currentUser, redactedFile, processingJob, AuditLogAction.FILE_REDACTED, "PDF File successfully redacted", AuditLogStatus.COMPLETED);

            return findPDFDocumentById(redactedFile.getId());

        } catch (Exception e) {
            processingJobService.markAsFailed(processingJob, e.getMessage());
            log.error("Failed to redact pdfFile | user_id: {} | file_id: {} | error: {}",
                    currentUser.getId(), pdfFile.getId(), e.getMessage());
            auditLogService.log(currentUser, pdfFile, processingJob, AuditLogAction.FILE_REDACTED, e.getMessage(), AuditLogStatus.FAILED);
            throw new RuntimeException("Failed to redact pdfFile", e);
        }
    }

    public PDFFileDetailedReadDTO encrypt(@NotNull Long fileId, PDFEncryptionSettingsDTO encryptionSettingsDTO) {

        User currentUser = sharedService.getCurrentUser();

        PDFFile pdfFile = pdfFileRepository.findByIdAndUserId(fileId, currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Document not found | user: {} | pdfFile: {}", currentUser.getUsername(), fileId);
                    return new EntityNotFoundException("Document not found | user: " + currentUser.getUsername());
                });

        PDFFile encryptedFile = null;

        ProcessingJob processingJob = processingJobService.create(
                ProcessingJobAction.FILE_ENCRYPTION,
                currentUser,
                pdfFile,
                ZonedDateTime.now());

        try {
            byte[] fileToEncrypt = s3Service.downloadDocument(pdfFile.getS3Key());

            String key = s3Service.uploadDocument(
                    encryptFile(fileToEncrypt, encryptionSettingsDTO),
                    pdfFile.getName(),
                    currentUser,
                    "processed");

            encryptedFile = pdfFileRepository.save(PDFFile.builder()
                    .name(pdfFile.getName())
                    .originalS3Key(pdfFile.getOriginalS3Key())
                    .s3Key(key)
                    .parentFile(pdfFile)
                    .version(pdfFile.getVersion() + 1L)
                    .user(currentUser)
                    .encryptionSettings(pdfEncryptionSettingsMapper.toEntity(encryptionSettingsDTO))
                    .build());

            processingJobService.maskAsDone(processingJob, key, encryptionSettingsDTO.toString());

            log.info("Document encrypt successfully | user_id: {} | file_id: {}", currentUser.getId(), pdfFile.getId());
            auditLogService.log(currentUser, encryptedFile, processingJob, AuditLogAction.FILE_REDACTED, "PDF File successfully encrypt", AuditLogStatus.COMPLETED);

            return findPDFDocumentById(encryptedFile.getId());

        } catch (Exception e) {
            processingJobService.markAsFailed(processingJob, e.getMessage());
            log.error("Failed to encrypt pdfFile | user_id: {} | file_id: {} | error: {}",
                    currentUser.getId(), pdfFile.getId(), e.getMessage());
            auditLogService.log(currentUser, pdfFile, processingJob, AuditLogAction.FILE_REDACTED, e.getMessage(), AuditLogStatus.FAILED);
            throw new RuntimeException("Failed to encrypt pdfFile", e);
        }
    }

    public PDFFileSummarizeResponseDTO summarize(@NotNull Long fileId, PDFFileSummarizeDTO summarizeDTO) {

        User currentUser = sharedService.getCurrentUser();

        PDFFile pdfFile = pdfFileRepository.findByIdAndUserId(fileId, currentUser.getId())
                .orElseThrow(() -> {
                    log.error("Document not found | user: {} | pdfFile: {}", currentUser.getUsername(), fileId);
                    return new EntityNotFoundException("Document not found | user: " + currentUser.getUsername());
                });

        ProcessingJob processingJob = processingJobService.create(
                ProcessingJobAction.FILE_SUMMARIZE,
                currentUser,
                pdfFile,
                ZonedDateTime.now());

        try {
            String fileText = extractDocumentText(s3Service.downloadDocument(pdfFile.getS3Key()));

            String summarizedText = aiService.summarize(fileText, summarizeDTO.getPrompt());

            processingJobService.maskAsDone(processingJob, pdfFile.getS3Key(), summarizedText);
            log.info("Document summarized successfully | user_id: {} | file_id: {}", currentUser.getId(), pdfFile.getId());
            auditLogService.log(currentUser, pdfFile, processingJob, AuditLogAction.FILE_SUMMARIZE, "PDF File successfully summarized", AuditLogStatus.COMPLETED);

            return PDFFileSummarizeResponseDTO.builder()
                    .fileId(pdfFile.getId())
                    .response(summarizedText)
                    .build();

        } catch (Exception e) {
            processingJobService.markAsFailed(processingJob, e.getMessage());
            log.error("Failed summarize from pdfFile | user_id: {} | file_id: {} | error: {}",
                    currentUser.getId(), pdfFile.getId(), e.getMessage());
            auditLogService.log(currentUser, pdfFile, processingJob, AuditLogAction.FILE_SUMMARIZE, e.getMessage(), AuditLogStatus.FAILED);
            throw new RuntimeException("Failed to summarize pdfFile", e);
        }
    }

    @Transactional(readOnly = true)
    public PDFFileDetailedReadDTO findPDFDocumentById(@NotNull Long fileId) {

        User currentUser = sharedService.getCurrentUser();

        PDFFile pdfFile = pdfFileRepository.findByIdAndUserId(fileId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Document with id " + fileId + " not found"));

        return PDFFileDetailedReadDTO.builder()
                .id(pdfFile.getId())
                .name(pdfFile.getName())
                .preSignedURL(s3Service.generatePreSignedURL(pdfFile.getS3Key()))
                .version(pdfFile.getVersion())
                .createdAt(pdfFile.getCreatedAt())
                .updatedAt(pdfFile.getUpdatedAt())
                .user(userReadMapper.toDto(currentUser))
                .encryptionSettings(pdfFile.getEncryptionSettings())
                .build();
    }

    @Transactional(readOnly = true)
    public PageDTO<PDFFileReadDTO> findAllPDFFiles(Pageable pageable) throws EntityNotFoundException {

        Page<PDFFile> pdfFilePage = pdfFileRepository.findAll(pageable);

        return pdfDocumentReadMapper.toPageDto(pdfFilePage);
    }

    @Transactional(readOnly = true)
    public PageDTO<PDFFileReadDTO> findAllPDFFilesByUserId(Pageable pageable) throws EntityNotFoundException {

        User currentUser = sharedService.getCurrentUser();

        Page<PDFFile> pdfFilePage = pdfFileRepository.findAllByUserId(currentUser.getId(), pageable);

        return pdfDocumentReadMapper.toPageDto(pdfFilePage);
    }


    public void deletePDFDocument(@NotNull Long fileId) throws IOException {

        PDFFile document = pdfFileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("Document with id " + fileId + " not found"));

        try {
            s3Service.deleteDocument(document.getS3Key());
            pdfFileRepository.deleteById(document.getId());
            log.info("Document with ID {{}} deleted successfully", fileId);
        } catch (Exception e) {
            auditLogService.log(document.getUser(), document, AuditLogAction.FILE_DELETE, "PDF File successfully deleted", AuditLogStatus.COMPLETED);
            throw new IOException("Failed to delete PDF document with ID " + fileId, e);
        }
    }

    private String extractDocumentText(byte[] fileBytes) {

        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(fileBytes));
            PdfDocument pdfDocument = new PdfDocument(reader);
            StringBuilder text = new StringBuilder();

            for (int page = 1; page <= pdfDocument.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(page)));
            }

            pdfDocument.close();

            return text.toString();

        } catch (IOException e) {
            log.error("Failed to extract text from PDF");
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    private byte[] redactPhrases(byte[] fileBytes, List<String> phrasesToRedact) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfReader reader = new PdfReader(new ByteArrayInputStream(fileBytes));
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(reader, writer);
            int pageCount = pdfDocument.getNumberOfPages();

            List<PdfCleanUpLocation> cleanUpLocations = new ArrayList<>();

            PdfDocumentContentParser parser = new PdfDocumentContentParser(pdfDocument);

            for (String phrase : phrasesToRedact) {
                for (int page = 1; page <= pageCount; page++) {
                    RegexBasedLocationExtractionStrategy strategy = new RegexBasedLocationExtractionStrategy(
                            Pattern.quote(phrase)
                    );

                    parser.processContent(page, strategy);

                    for (IPdfTextLocation location :
                            strategy.getResultantLocations()) {

                        cleanUpLocations.add(
                                new PdfCleanUpLocation(
                                        page,
                                        location.getRectangle(),
                                        ColorConstants.BLACK
                                )
                        );
                    }
                }
            }

            if (!cleanUpLocations.isEmpty()) {
                PdfCleanUpTool cleaner =
                        new PdfCleanUpTool(
                                pdfDocument,
                                cleanUpLocations,
                                new CleanUpProperties()
                        );

                cleaner.cleanUp();
            }

            pdfDocument.close();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to redact PDF", e);
            throw new RuntimeException("Failed to redact PDF", e);
        }
    }

    private byte[] encryptFile(byte[] fileBytes, PDFEncryptionSettingsDTO encryptionDTO){
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfReader reader = new PdfReader(new ByteArrayInputStream(fileBytes));
            WriterProperties properties = new WriterProperties();

            properties.setStandardEncryption(
                    encryptionDTO.getUserPass().getBytes(),
                    encryptionDTO.getOwnerPass().getBytes(),
                    setEncryptPermissions(encryptionDTO),
                        EncryptionConstants.ENCRYPTION_AES_256 | EncryptionConstants.DO_NOT_ENCRYPT_METADATA);
            PdfWriter writer = new PdfWriter(outputStream, properties);

            PdfDocument pdfDocument = new PdfDocument(reader, writer);

            pdfDocument.close();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to set permissions in PDF", e);
            throw new RuntimeException("Failed to set permissions in PDF", e);
        }
    }

    private int setEncryptPermissions(PDFEncryptionSettingsDTO encryptionDTO) throws IOException {

        return (encryptionDTO.isAllowPrinting() ? EncryptionConstants.ALLOW_PRINTING | EncryptionConstants.ALLOW_DEGRADED_PRINTING : 0)
                | (encryptionDTO.isAllowModifyContents() ? EncryptionConstants.ALLOW_MODIFY_CONTENTS : 0)
                | (encryptionDTO.isAllowCopy() ? EncryptionConstants.ALLOW_COPY : 0)
                | (encryptionDTO.isAllowModifyAnnotations() ? EncryptionConstants.ALLOW_MODIFY_ANNOTATIONS : 0)
                | (encryptionDTO.isAllowFillIn() ? EncryptionConstants.ALLOW_FILL_IN : 0)
                | (encryptionDTO.isAllowScreenReaders() ? EncryptionConstants.ALLOW_SCREENREADERS : 0)
                | (encryptionDTO.isAllowAssembly() ? EncryptionConstants.ALLOW_ASSEMBLY : 0);
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > PDFFileValidation.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds 10 MB");
        }

        if (!PDFFileValidation.ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file extension");
        }
    }
}