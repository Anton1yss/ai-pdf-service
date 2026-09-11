package by.AntonDemchuk.ai_pdf_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private PDFFile pdfFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private ProcessingJobAction action;

    @Column(name = "response_message")
    private String responseMessage;

    @Size(max = 512)
    @Column(name = "result_s3_key")
    private String resultS3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProcessingJobStatus status;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;
}