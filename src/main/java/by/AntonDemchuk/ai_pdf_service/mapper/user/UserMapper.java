package by.AntonDemchuk.ai_pdf_service.mapper.user;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserDTO;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class UserMapper implements BaseMapper<User, UserDTO> {

    public abstract UserDTO toDto(User user);

    public abstract User toEntity(UserDTO userDto);

    public abstract void updateFromDtoToEntity(UserDTO fromDto, @MappingTarget User toEntity);

    public User update(UserDTO fromDto, User toEntity) {
        updateFromDtoToEntity(fromDto, toEntity);
        return toEntity;
    }
}