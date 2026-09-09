package by.AntonDemchuk.ai_pdf_service.repository;

import by.AntonDemchuk.ai_pdf_service.entity.PDFFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PDFFileRepository extends JpaRepository<PDFFile, Long> {

    Page<PDFFile> findAllByUserId(Long userId, Pageable pageable);

    Optional<PDFFile> findByIdAndUserId(Long id, Long userId);

    Boolean existsByIdAndUserId(Long id, Long userId);
}