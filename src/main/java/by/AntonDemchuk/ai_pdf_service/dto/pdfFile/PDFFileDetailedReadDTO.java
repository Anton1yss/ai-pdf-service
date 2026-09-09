package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobReadDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class PDFFileDetailedReadDTO {
    private Long id;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private Long version;
    private String preSignedURL;
    private UserReadDTO user;
    private List<ProcessingJobReadDTO> processingJobReadDTOList;
}