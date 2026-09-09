package by.AntonDemchuk.ai_pdf_service.dto.user;

import by.AntonDemchuk.ai_pdf_service.entity.UserRole;
import lombok.*;

import java.time.ZonedDateTime;

@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UserReadDTO {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private ZonedDateTime createdAt;
}