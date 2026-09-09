package by.AntonDemchuk.ai_pdf_service.mapper.processingJob;

import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobDTO;
import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJob;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProcessingJobMapper extends BaseMapper<ProcessingJob, ProcessingJobDTO> {
}
