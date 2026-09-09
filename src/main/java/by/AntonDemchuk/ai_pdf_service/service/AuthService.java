package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserLoginDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserRegisterDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.exception.RegistrationException;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserRegisterMapper;
import by.AntonDemchuk.ai_pdf_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserRegisterMapper userRegisterMapper;

    private final AuditLogService auditLogService;

    public User signup(UserRegisterDTO dto) {

        User userToRegister = userRegisterMapper.toEntity(dto);

        System.out.println(userToRegister.toString());

        if(dto.getPassword().equals(dto.getConfirmPassword())) {
            userToRegister.setPassword(passwordEncoder.encode(dto.getPassword()));

        } else throw new RegistrationException("Passwords do not match.");

        User newUser = userRepository.save(userToRegister);

        auditLogService.log(newUser, AuditLogAction.USER_REGISTER, "User successfully registered", AuditLogStatus.COMPLETED);

        return newUser;
    }

    public User authenticate(UserLoginDTO input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getUsername(),
                        input.getPassword()
                )
        );

        User userToAuth = userRepository.findByUsername(input.getUsername())
                .orElseThrow();

        auditLogService.log(userToAuth, AuditLogAction.USER_LOGIN, "User successfully logged", AuditLogStatus.COMPLETED);

        return userToAuth;
    }
}