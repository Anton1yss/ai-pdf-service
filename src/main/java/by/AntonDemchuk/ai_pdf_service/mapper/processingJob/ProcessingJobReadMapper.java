package by.AntonDemchuk.ai_pdf_service.mapper.processingJob;

import by.AntonDemchuk.ai_pdf_service.dto.processingJob.ProcessingJobReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJob;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ProcessingJobReadMapper extends BaseMapper<ProcessingJob, ProcessingJobReadDTO> {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fileId", source = "pdfFile.id")
    public abstract ProcessingJobReadDTO toDto(ProcessingJob processingJob);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "pdfFile", ignore = true)
    public abstract ProcessingJob toEntity(ProcessingJobReadDTO reactionReadDto);

}
