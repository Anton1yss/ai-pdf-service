package by.AntonDemchuk.ai_pdf_service.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private Long expiresIn;
}