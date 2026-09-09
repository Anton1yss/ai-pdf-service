package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Transactional
@RequiredArgsConstructor
@Validated
@Slf4j
public class SharedService {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("User is not authenticated");
            throw new NullPointerException("No authenticated user found");
        }

        User principal = (User) authentication.getPrincipal();

        if(principal == null) {
            log.info("User is not found");
            throw new EntityNotFoundException("User not found");
        }

        return principal;
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}