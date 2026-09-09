package by.AntonDemchuk.ai_pdf_service.mapper.pdfFile;

import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFileReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.PDFFile;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PDFFileReadMapper extends BaseMapper<PDFFile, PDFFileReadDTO> {
}
