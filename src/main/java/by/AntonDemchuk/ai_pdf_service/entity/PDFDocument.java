package by.AntonDemchuk.ai_pdf_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "redactionJobsList")
@EqualsAndHashCode(exclude = "redactionJobsList")
@Table(name = "pdf_documents")
public class PDFDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 2, max = 128)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 256)
    @Column(name = "description")
    private String description;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "pdfDocument")
    private List<RedactionJob> redactionJobsList = new ArrayList<>();
}