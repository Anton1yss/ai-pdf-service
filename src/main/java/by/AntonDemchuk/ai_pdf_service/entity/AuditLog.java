package by.AntonDemchuk.ai_pdf_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private PDFDocument pdfDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redaction_job_id")
    private RedactionJob redactionJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private AuditLogAction action;

    @Size(max = 512)
    @Column(name = "message")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AuditLogStatus status;

    @Column(name = "created_at")
    @CreationTimestamp
    private ZonedDateTime createdAt;
}