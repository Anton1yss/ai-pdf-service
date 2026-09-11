package by.AntonDemchuk.ai_pdf_service.mapper.pdfFile;

import by.AntonDemchuk.ai_pdf_service.dto.pdfFile.PDFEncryptionSettingsDTO;
import by.AntonDemchuk.ai_pdf_service.entity.PDFEncryptionSettings;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PDFFileEncryptionSettingsMapper extends BaseMapper<PDFEncryptionSettings, PDFEncryptionSettingsDTO> {

    @Mapping(target = "encrypted", constant = "true")
    PDFEncryptionSettings toEntity(PDFEncryptionSettingsDTO dto);

}