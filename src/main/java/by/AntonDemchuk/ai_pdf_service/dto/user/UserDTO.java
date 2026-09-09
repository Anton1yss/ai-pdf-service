package by.AntonDemchuk.ai_pdf_service.dto.user;

import by.AntonDemchuk.ai_pdf_service.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {

    @Email(message = "Email's format is wrong.")
    @NotNull(message = "Email cannot be empty.")
    private String email;

    @Size(min = 4, max = 32, message = "Password size should be between 4 and 32 symbols.")
    private String password;

}
