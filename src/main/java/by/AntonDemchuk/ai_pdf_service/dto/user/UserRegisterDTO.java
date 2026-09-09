package by.AntonDemchuk.ai_pdf_service.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class UserRegisterDTO {

    @NotNull(message = "Username cannot be empty.")
    private String username;

    @Email(message = "Email's format is wrong.")
    @NotNull(message = "Email cannot be empty.")
    private String email;

    @Size(min = 4, max = 32, message = "Password size should be between 4 and 32 symbols.")
    @NotNull(message = "Password cannot be empty.")
    private String password;

    private String confirmPassword;

    private ZonedDateTime createdAt;
}
