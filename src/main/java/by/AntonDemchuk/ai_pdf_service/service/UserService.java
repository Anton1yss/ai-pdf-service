package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Transactional
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserReadMapper userReadMapper;

    private final SharedService sharedService;
    private final AuditLogService auditLogService;

    private final PasswordEncoder passwordEncoder;

    public void delete(@NotNull Long id) throws EntityNotFoundException {
        User currentUser = sharedService.getCurrentUser();

        if (currentUser.getId().equals(id)) {
            log.info("User (ID: {}) deleted successfully.", currentUser.getId());
            auditLogService.log(currentUser,
                    AuditLogAction.USER_DELETE,
                    "User with ID: " + currentUser.getId() + " was deleted successfully",
                    AuditLogStatus.COMPLETED);
            userRepository.deleteById(currentUser.getId());
        }
    }

    public UserReadDTO update(@Valid UserDTO userToUpdateDto) {

        User currentUser = sharedService.getCurrentUser();

        User userToUpdate = userRepository.findById(currentUser.getId())
                .map(entity -> {
                    userMapper.update(userToUpdateDto, entity);
                    entity.setPassword(passwordEncoder.encode(userToUpdateDto.getPassword()));
                    return entity;
                })
                .map(userRepository::save)
                .orElseThrow(() -> new EntityNotFoundException("User (ID: " + currentUser.getId() + ") not found."));


        log.info("User (ID: {}) updated successfully.", currentUser.getId());
        auditLogService.log(currentUser,
                AuditLogAction.USER_UPDATE,
                "User with ID: " + currentUser.getId() + " was updated successfully",
                AuditLogStatus.COMPLETED);

        return userReadMapper.toDto(userToUpdate);
    }

    @Transactional(readOnly = true)
    public UserReadDTO findById(@NotNull Long userId) {
        return userRepository.findById(userId)
                .map(userReadMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User (ID: " + userId + ") not found."));
    }

    @Transactional(readOnly = true)
    public PageDTO<UserReadDTO> findAll(Pageable pageable) {
        Page<User> usersPage = userRepository.findAll(pageable);

        return userReadMapper.toPageDto(usersPage);
    }
}
