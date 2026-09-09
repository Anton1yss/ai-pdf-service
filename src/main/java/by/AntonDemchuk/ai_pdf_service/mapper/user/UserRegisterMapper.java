package by.AntonDemchuk.ai_pdf_service.mapper.user;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserRegisterDTO;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserRegisterMapper extends BaseMapper<User, UserRegisterDTO> {
}
