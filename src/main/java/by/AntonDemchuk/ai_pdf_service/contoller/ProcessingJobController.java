package by.AntonDemchuk.ai_pdf_service.contoller;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobReadDTO;
import by.AntonDemchuk.ai_pdf_service.service.ProcessingJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/processingJob")
@Tag(name = "Processing Job Controller")
public class ProcessingJobController {

    private final ProcessingJobService processingJobService;

    @GetMapping(value = "/{processingJobId}")
    @Operation(summary = "Get processing job by ID", description = "Returns detailed information about a processing job identified by its ID.")
    public ResponseEntity<ProcessingJobReadDTO> findById(@PathVariable Long processingJobId) {
        return ResponseEntity.ok(processingJobService.findById(processingJobId));
    }

    @GetMapping(value = "/file/{fileId}")
    @Operation(summary = "Get processing jobs for a file", description = "Returns a paginated list of processing jobs associated with the specified PDF file.")
    public ResponseEntity<PageDTO<ProcessingJobReadDTO>> findAllProcessingJobs(
            @PathVariable Long fileId,
            @ParameterObject @PageableDefault(size = 5, page = 0, sort = {}) Pageable pageable) {

        return ResponseEntity.ok(processingJobService.findByAllByFileId(fileId, pageable));
    }

}
