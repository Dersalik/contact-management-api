package com.salik.contactmanagementapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.salik.contactmanagementapi.dto.AuthDTO;
import com.salik.contactmanagementapi.model.User;
import com.salik.contactmanagementapi.repository.UserRepository;
import com.salik.contactmanagementapi.security.CustomAuthenticationManager;
import com.salik.contactmanagementapi.security.JwtUtil;
import com.salik.contactmanagementapi.security.UserAuthentication;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomAuthenticationManager customAuthenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;
    private AuthDTO.LoginRequest loginRequest;
    private AuthDTO.RegisterRequest registerRequest;
    private Authentication authentication;

    @BeforeEach
    public void setup() {
        // Setup User
        ObjectId userId = new ObjectId();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");

        List<String> roles = new ArrayList<>();
        roles.add(User.Role.ROLE_USER.name());
        user.setRoles(roles);

        // Setup Login Request
        loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Setup Register Request
        registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");

        // Setup Authentication
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        authentication = new UserAuthentication(
                user.getId().toHexString(),
                user.getUsername(),
                authorities
        );
    }

    @Test
    public void testLogin_Success() {
        when(customAuthenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.just(authentication));
        when(userRepository.findByUsername(anyString())).thenReturn(Mono.just(user));
        when(jwtUtil.generateToken(any(), any())).thenReturn("jwtToken");

        StepVerifier.create(authService.login(loginRequest))
                .expectNextMatches(response ->
                        response.getToken().equals("jwtToken") &&
                                response.getUsername().equals("testuser") &&
                                response.getEmail().equals("test@example.com") &&
                                response.getId().equals(user.getId().toHexString()) &&
                                response.getRoles().contains(User.Role.ROLE_USER.name())
                )
                .verifyComplete();
    }

    @Test
    public void testRegister_Success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(customAuthenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.just(authentication));
        when(userRepository.findByUsername(anyString())).thenReturn(Mono.just(user));
        when(jwtUtil.generateToken(any(), any())).thenReturn("jwtToken");

        StepVerifier.create(authService.register(registerRequest))
                .expectNextMatches(response ->
                        response.getToken().equals("jwtToken") &&
                                response.getUsername().equals("testuser")
                )
                .verifyComplete();
    }

    @Test
    public void testRegister_UsernameExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(registerRequest))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    public void testRegister_EmailExists() {
        when(userRepository.existsByUsername(anyString())).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(registerRequest))
                .expectError(ResponseStatusException.class)
                .verify();
    }
}