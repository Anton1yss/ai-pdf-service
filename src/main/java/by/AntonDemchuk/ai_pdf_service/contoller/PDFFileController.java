package by.AntonDemchuk.ai_pdf_service.contoller;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFIleDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileDetailedReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobDTO;
import by.AntonDemchuk.ai_pdf_service.service.PDFFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pdfFile")
@Tag(name = "PDF Files", description = "Endpoints for uploading, retrieving, deleting and redacting PDF files")
public class PDFFileController {

    private final PDFFileService pdfFileService;

    @RequestMapping(value="", method=RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF file", description = "Uploads a PDF file and stores it in the system.")
    public ResponseEntity<PDFFIleDTO> createFile(@RequestPart MultipartFile file) throws IOException {
        return ResponseEntity.ok(pdfFileService.create(file));
    }

    @RequestMapping(value="/{fileId}", method=RequestMethod.GET)
    @Operation(summary = "Get PDF file by ID", description = "Returns detailed information about a PDF file identified by its ID.")
    public ResponseEntity<PDFFileDetailedReadDTO> findDocumentById(@PathVariable Long fileId) {
        return ResponseEntity.ok(pdfFileService.findPDFDocumentById(fileId));
    }

    @RequestMapping(value="/my", method=RequestMethod.GET)
    @Operation(summary = "Get PDF files by Authorized User", description = "Returns detailed information about a PDF files identified by Authorized User's ID.")
    public ResponseEntity<PageDTO<PDFFileReadDTO>> findAllFilesByUserId(
            @ParameterObject @PageableDefault(size = 5, page = 0, sort = {}) Pageable pageable) {

        return ResponseEntity.ok(pdfFileService.findAllPDFFilesByUserId(pageable));
    }

    @RequestMapping(value="/{fileId}", method=RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Delete a PDF file", description = "Deletes a PDF file identified by its ID.")
    public void deleteDocumentById(@PathVariable Long fileId) throws IOException {
        pdfFileService.deletePDFDocument(fileId);
    }

    @RequestMapping(value="/{fileId}/redact", method=RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Redact sensitive information", description = "Analyzes the PDF file and hides sensitive information specified by the processing job.")
    public void hideFileInformation(@PathVariable Long fileId, @RequestBody ProcessingJobDTO processingJobDTO) {
        pdfFileService.redactContent(fileId, processingJobDTO);
    }

}