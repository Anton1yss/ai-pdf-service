package by.AntonDemchuk.ai_pdf_service.mapper.auditLog;

import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLog;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogReadMapper extends BaseMapper<AuditLog, AuditLogReadDTO> {
}
