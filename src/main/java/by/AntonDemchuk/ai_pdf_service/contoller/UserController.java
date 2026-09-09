package by.AntonDemchuk.ai_pdf_service.contoller;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User Controller")
public class UserController {

    private final UserService userService;

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete current user", description = "Deletes the account of the currently authenticated user.")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) throws EntityNotFoundException {
        userService.delete(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("")
    @Operation(summary = "Update current user", description = "Updates the information of the currently authenticated user.")
    public ResponseEntity<UserReadDTO> updateUser(@RequestBody @Valid UserDTO userDto) {
        return ResponseEntity.ok(userService.update(userDto));
    }

    /// TODO: add /my
}