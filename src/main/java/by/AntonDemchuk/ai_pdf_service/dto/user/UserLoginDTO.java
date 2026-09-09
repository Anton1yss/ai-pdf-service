package by.AntonDemchuk.ai_pdf_service.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginDTO {

    @NotNull(message = "Username cannot be empty.")
    private String username;

    @NotNull(message = "Password cannot be empty.")
    private String password;
}
