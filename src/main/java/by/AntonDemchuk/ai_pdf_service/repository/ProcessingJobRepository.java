package by.AntonDemchuk.ai_pdf_service.repository;

import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob,Long> {

    Page<ProcessingJob> findAllByPdfFile_Id(Long pdfFileId, Pageable pageable);

    Optional<ProcessingJob> findAllByIdAndUserId(Long id, Long userId);


}
