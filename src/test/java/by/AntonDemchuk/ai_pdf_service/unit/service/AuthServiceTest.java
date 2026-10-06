package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.dto.user.UserLoginDTO;
import by.AntonDemchuk.ai_pdf_service.dto.user.UserRegisterDTO;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogAction;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLogStatus;
import by.AntonDemchuk.ai_pdf_service.entity.User;
import by.AntonDemchuk.ai_pdf_service.exception.RegistrationException;
import by.AntonDemchuk.ai_pdf_service.mapper.user.UserRegisterMapper;
import by.AntonDemchuk.ai_pdf_service.repository.UserRepository;
import by.AntonDemchuk.ai_pdf_service.service.AuditLogService;
import by.AntonDemchuk.ai_pdf_service.service.AuthService;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest extends BaseServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRegisterMapper userRegisterMapper;

    @Mock
    private AuditLogService auditLogService;

    @Test
    public void signup_shouldRegisterUser_andEncodePassword() {

        /* Arranging */
        UserRegisterDTO dto = mock(UserRegisterDTO.class);

        when(dto.getPassword()).thenReturn("password");
        when(dto.getConfirmPassword()).thenReturn("password");
        when(userRegisterMapper.toEntity(dto)).thenReturn(mockedUser);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(mockedUser)).thenReturn(mockedUser);

        /* Acting */
        User res = authService.signup(dto);

        /* Asserting & Verifying */
        assertEquals(mockedUser, res);
        verify(mockedUser).setPassword("encoded-password");
        verify(userRepository).save(mockedUser);
        verify(auditLogService).log(eq(mockedUser), eq(AuditLogAction.USER_REGISTER), anyString(), eq(AuditLogStatus.COMPLETED));
    }

    @Test
    public void signup_shouldThrow_whenPasswordsDoNotMatch() {

        /* Arranging */
        UserRegisterDTO dto = mock(UserRegisterDTO.class);

        when(dto.getPassword()).thenReturn("password");
        when(dto.getConfirmPassword()).thenReturn("different");
        when(userRegisterMapper.toEntity(dto)).thenReturn(mockedUser);

        /* Acting & Asserting */
        RegistrationException ex = assertThrows(RegistrationException.class, () -> authService.signup(dto));
        assertEquals("Passwords do not match.", ex.getMessage());

        /* Verifying */
        verifyNoInteractions(passwordEncoder, userRepository, auditLogService);
        verify(mockedUser, never()).setPassword(anyString());
    }

    @Test
    public void authenticate_shouldAuthenticateAndReturnUser() {

        /* Arranging */
        UserLoginDTO dto = mock(UserLoginDTO.class);

        when(dto.getUsername()).thenReturn("username");
        when(dto.getPassword()).thenReturn("password");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(mockedUser));

        /* Acting */
        User res = authService.authenticate(dto);

        /* Asserting & Verifying */
        assertEquals(mockedUser, res);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, captor.getValue());
        assertEquals("username", captor.getValue().getPrincipal());
        assertEquals("password", captor.getValue().getCredentials());

        verify(userRepository).findByUsername("username");
        verify(auditLogService).log(eq(mockedUser), eq(AuditLogAction.USER_LOGIN), anyString(), eq(AuditLogStatus.COMPLETED));
    }

    @Test
    public void authenticate_shouldThrow_whenCredentialsAreInvalid() {

        /* Arranging */
        UserLoginDTO dto = mock(UserLoginDTO.class);

        when(dto.getUsername()).thenReturn("username");
        when(dto.getPassword()).thenReturn("wrong");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        /* Acting & Asserting */
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(dto));

        /* Verifying */
        verifyNoInteractions(userRepository, auditLogService);
    }

    @Test
    public void authenticate_shouldThrow_whenUserNotFoundAfterAuthentication() {

        /* Arranging */
        UserLoginDTO dto = mock(UserLoginDTO.class);

        when(dto.getUsername()).thenReturn("username");
        when(dto.getPassword()).thenReturn("password");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());

        /* Acting & Asserting */
        assertThrows(NoSuchElementException.class, () -> authService.authenticate(dto));

        /* Verifying */
        verifyNoInteractions(auditLogService);
    }
}