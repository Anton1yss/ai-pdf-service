package by.AntonDemchuk.ai_pdf_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "processingJobsList")
@EqualsAndHashCode(exclude = "processingJobsList")
@Table(name = "pdf_files")
public class PDFFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 2, max = 128)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(min = 2, max = 512)
    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Size(max = 512)
    @Column(name = "original_s3_key")
    private String originalS3Key;

    @Column(name = "created_at")
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private ZonedDateTime updatedAt;

    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_file_id")
    private PDFFile parentFile;

    @Column(name = "encryption_settings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private PDFEncryptionSettings encryptionSettings;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "pdfFile")
    private List<ProcessingJob> processingJobsList = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "pdfFile")
    private List<AuditLog> auditLogList = new ArrayList<>();
}