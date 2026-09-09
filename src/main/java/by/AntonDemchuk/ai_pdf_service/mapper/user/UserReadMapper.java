package by.AntonDemchuk.ai_pdf_service.mapper.user;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserReadMapper extends BaseMapper<User, UserReadDTO> {
}
