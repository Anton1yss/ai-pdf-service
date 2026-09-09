package by.AntonDemchuk.ai_pdf_service.contoller;

import by.AntonDemchuk.ai_pdf_service.dto.LoginResponseDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserLoginDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserRegisterDTO;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.service.AuthService;
import by.AntonDemchuk.ai_pdf_service.service.JWTService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth Controller")
public class AuthController {

    private final JWTService jwtService;
    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Creates a new user account using the provided registration information.")
    public ResponseEntity<User> register(@Valid @RequestBody UserRegisterDTO registerUserDto) {
        User registeredUser = authService.signup(registerUserDto);

        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates a user and returns a JWT token that can be used to access protected endpoints.")
    public ResponseEntity<LoginResponseDTO> authenticate(@Valid @RequestBody UserLoginDTO loginUserDto) {
        User authenticatedUser = authService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setToken(jwtToken);
        loginResponseDTO.setExpiresIn(loginResponseDTO.getExpiresIn());

        return ResponseEntity.ok(loginResponseDTO);
    }
}
