package by.AntonDemchuk.ai_pdf_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "redaction_jobs")
public class RedactionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private PDFDocument pdfDocument;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "response_message")
    private String responseMessage;

    @Size(max = 512)
    @Column(name = "result_s3_key")
    private String resultS3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RedactionJobStatus status;

    @Column(name = "created_at")
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;
}