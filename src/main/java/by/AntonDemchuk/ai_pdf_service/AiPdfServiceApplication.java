package by.AntonDemchuk.ai_pdf_service;

import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.entity.UserRole;
import by.AntonDemchuk.ai_pdf_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@RequiredArgsConstructor
public class AiPdfServiceApplication implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(AiPdfServiceApplication.class, args);
    }

    /* Create a test User */
    @Override
    public void run(String... args) {
        /*userRepository.save(User.builder()
                .username("Victory")
                .email("victory3749@gmail.com")
                .password(passwordEncoder.encode("victory3749"))
                .role(UserRole.ADMIN)
                .build());*/
    }
}
