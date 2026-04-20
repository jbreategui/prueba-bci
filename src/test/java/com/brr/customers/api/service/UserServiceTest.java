package com.brr.customers.api.service;

import com.brr.customers.api.domain.User;
import com.brr.customers.api.dto.PhoneRequest;
import com.brr.customers.api.dto.SignUpRequest;
import com.brr.customers.api.dto.SignUpResponse;
import com.brr.customers.api.exception.EmailAlreadyRegisteredException;
import com.brr.customers.api.exception.UserNotFoundException;
import com.brr.customers.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private SignUpRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new SignUpRequest(
                "Juan Perez",
                "juan@bci.cl",
                "Abcdef12",
                List.of(new PhoneRequest("1234567", "1", "57"))
        );
    }

    @Test
    void signUp_withNewEmail_createsUserSuccessfully() {
        when(userRepository.findByEmail("juan@bci.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Abcdef12")).thenReturn("hashedPassword");
        when(jwtService.generate(any(User.class))).thenReturn("jwt.token.here");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SignUpResponse response = userService.signUp(validRequest);

        assertThat(response.email()).isEqualTo("juan@bci.cl");
        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.phones()).hasSize(1);
    }

    @Test
    void signUp_withExistingEmail_throwsException() {
        when(userRepository.findByEmail("juan@bci.cl")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.signUp(validRequest))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("El correo ya registrado");
    }

    @Test
    void findById_whenUserNotFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuario no encontrado");
    }
}
