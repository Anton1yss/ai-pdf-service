package by.AntonDemchuk.ai_pdf_service.dto.processingJob;

import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJobStatus;
import lombok.*;

import java.time.ZonedDateTime;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ProcessingJobReadDTO {
    private Long id;
    private Long userId;
    private Long fileId;
    private String prompt;
    private String responseMessage;
    private ProcessingJobStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime completedAt;
}
