package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFIleDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileDetailedReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobDTO;

import by.AntonDemchuk.ai_pdf_service.entity.*;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.pdfFile.PDFFileReadMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.PDFFileRepository;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
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
                .build());

        log.info("Document created successfully | user: {} | document: {}", currentUser.getUsername(), document.getName());
        auditLogService.log(currentUser, fileToCreate, AuditLogAction.FILE_SAVE, "PDF File successfully created", AuditLogStatus.COMPLETED);

        return pdfDocumentMapper.toDto(fileToCreate);
    }

    public PDFFileDetailedReadDTO redactContent(@NotNull Long fileId, ProcessingJobDTO processingJobDto) {

        User currentUser = sharedService.getCurrentUser();

        PDFFile pdfFile = pdfFileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("Document not found | user: {} | pdfFile: {}", currentUser.getUsername(), fileId);
                    return new EntityNotFoundException("Document not found | user: " + currentUser.getUsername());
                });

        ProcessingJob processingJob = processingJobService.create(
                processingJobDto,
                currentUser,
                pdfFile,
                ZonedDateTime.now());

        PDFFile redactedFile = null;

        try {
            byte[] fileToRedact = s3Service.downloadDocument(pdfFile.getS3Key());

            String fileText = extractDocumentText(fileToRedact);

            List<String> phrasesToRedact = aiService.getPhrasesToRedact(fileText, processingJobDto.getPrompt());

            String key = s3Service.uploadDocument(
                    redactPhrases(fileToRedact, phrasesToRedact),
                    pdfFile.getName(),
                    currentUser,
                    "redacted");

            redactedFile = pdfFileRepository.save(PDFFile.builder()
                    .name(pdfFile.getName())
                    .originalS3Key(pdfFile.getOriginalS3Key())
                    .s3Key(key)
                    .parentFile(pdfFile)
                    .version(pdfFile.getVersion() + 1L)
                    .user(currentUser)
                    .build());

            processingJobService.maskAsDone(processingJob, key, phrasesToRedact.toString());

            log.info("Document redacted successfully | user_id: {} | file_id: {}", currentUser.getId(), pdfFile.getId());
            auditLogService.log(currentUser, redactedFile, processingJob, AuditLogAction.FILE_REDACTED, "PDF File successfully redacted", AuditLogStatus.COMPLETED);

            return findPDFDocumentById(redactedFile.getId());

        } catch (Exception e) {
            processingJobService.markAsFailed(processingJob, e.getMessage());
            log.error("Failed to redact pdfFile | user_id: {} | file_id: {} | error: {}",
                    currentUser.getId(), pdfFile.getId(), e.getMessage());
            auditLogService.log(currentUser, redactedFile, processingJob, AuditLogAction.FILE_REDACTED, e.getMessage(), AuditLogStatus.FAILED);
            throw new RuntimeException("Failed to redact pdfFile", e);
        }
    }

    @Transactional(readOnly = true)
    public PDFFileDetailedReadDTO findPDFDocumentById(@NotNull Long fileId) {

        User currentUser = sharedService.getCurrentUser();

        PDFFile document = pdfFileRepository.findByIdAndUserId(fileId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Document with id " + fileId + " not found"));

        return PDFFileDetailedReadDTO.builder()
                .id(document.getId())
                .name(document.getName())
                .preSignedURL(s3Service.generatePreSignedURL(document.getS3Key()))
                .version(document.getVersion())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .user(userReadMapper.toDto(currentUser))
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