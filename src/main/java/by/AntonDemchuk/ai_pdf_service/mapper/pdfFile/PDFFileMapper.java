package by.AntonDemchuk.ai_pdf_service.mapper.pdfFile;

import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFFIleDTO;
import by.AntonDemchuk.ai_pdf_service.entity.PDFFile;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PDFFileMapper extends BaseMapper<PDFFile, PDFFIleDTO> {

    @Mapping(target = "userId", source = "user.id")
    public abstract PDFFIleDTO toDto(PDFFile pdfFile);

    public abstract PDFFile toEntity(PDFFIleDTO pdfFIleDTO);

}
