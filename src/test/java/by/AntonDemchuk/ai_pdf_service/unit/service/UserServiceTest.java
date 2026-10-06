package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.dto.PageDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserReadDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserMapper;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserReadMapper;
import by.AntonDemchuk.ai_pdf_service.repository.UserRepository;
import by.AntonDemchuk.ai_pdf_service.service.AuditLogService;
import by.AntonDemchuk.ai_pdf_service.service.SharedService;
import by.AntonDemchuk.ai_pdf_service.service.UserService;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest extends BaseServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserReadMapper userReadMapper;

    @Mock
    private SharedService sharedService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    public void delete_shouldDeleteCurrentUser() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);

        /* Acting */
        userService.delete(userId);

        /* Verifying */
        verify(auditLogService).log(eq(mockedUser), eq(AuditLogAction.USER_DELETE), anyString(), eq(AuditLogStatus.COMPLETED));
        verify(userRepository).deleteById(userId);
    }

    @Test
    public void delete_shouldDoNothing_whenIdIsNotCurrentUsersId() {

        /* Arranging */
        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);

        /* Acting */
        userService.delete(2L);

        /* Verifying */
        verify(userRepository, never()).deleteById(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    public void update_shouldUpdateUser_andEncodePassword() {

        /* Arranging */
        UserDTO userDto = mock(UserDTO.class);
        User userEntity = mock(User.class);
        UserReadDTO expectedDto = mock(UserReadDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userDto.getPassword()).thenReturn("raw-password");
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userReadMapper.toDto(userEntity)).thenReturn(expectedDto);

        /* Acting */
        UserReadDTO res = userService.update(userDto);

        /* Asserting & Verifying */
        assertEquals(expectedDto, res);
        verify(userMapper).update(userDto, userEntity);
        verify(userEntity).setPassword("encoded-password");
        verify(userRepository).save(userEntity);
        verify(auditLogService).log(eq(mockedUser), eq(AuditLogAction.USER_UPDATE), anyString(), eq(AuditLogStatus.COMPLETED));
        verify(userReadMapper).toDto(userEntity);
    }

    @Test
    public void update_shouldThrow_whenUserNotFound() {

        /* Arranging */
        UserDTO userDto = mock(UserDTO.class);

        when(sharedService.getCurrentUser()).thenReturn(mockedUser);
        when(mockedUser.getId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> userService.update(userDto));

        /* Verifying */
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper, passwordEncoder, auditLogService, userReadMapper);
    }

    @Test
    public void findById_shouldFindUser() {

        /* Arranging */
        UserReadDTO expectedDto = mock(UserReadDTO.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockedUser));
        when(userReadMapper.toDto(mockedUser)).thenReturn(expectedDto);

        /* Acting */
        UserReadDTO res = userService.findById(userId);

        /* Asserting & Verifying */
        assertEquals(expectedDto, res);
        verify(userRepository).findById(userId);
        verify(userReadMapper).toDto(mockedUser);
    }

    @Test
    public void findById_shouldThrow_whenUserNotFound() {

        /* Arranging */
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(EntityNotFoundException.class, () -> userService.findById(userId));

        /* Verifying */
        verifyNoInteractions(userReadMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findAll_shouldReturnPageDto() {

        /* Arranging */
        Page<User> mockedPage = mock(Page.class);
        PageDTO<UserReadDTO> expectedPageDto = mock(PageDTO.class);

        when(userRepository.findAll(pageable)).thenReturn(mockedPage);
        when(userReadMapper.toPageDto(mockedPage)).thenReturn(expectedPageDto);

        /* Acting */
        PageDTO<UserReadDTO> res = userService.findAll(pageable);

        /* Asserting & Verifying */
        assertEquals(expectedPageDto, res);
        verify(userRepository).findAll(pageable);
        verify(userReadMapper).toPageDto(mockedPage);
    }
}