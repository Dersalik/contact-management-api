package com.salik.contactmanagementapi.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.salik.contactmanagementapi.dto.AuthDTO;
import com.salik.contactmanagementapi.model.User;
import com.salik.contactmanagementapi.repository.UserRepository;
import com.salik.contactmanagementapi.security.CustomAuthenticationManager;
import com.salik.contactmanagementapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationManager customAuthenticationManager;
    private final JwtUtil jwtUtil;

    public Mono<AuthDTO.AuthResponse> login(AuthDTO.LoginRequest loginRequest) {
        return customAuthenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), loginRequest.getPassword()))
                .flatMap(this::generateAuthResponse);
    }

    public Mono<AuthDTO.AuthResponse> register(AuthDTO.RegisterRequest registerRequest) {
        return userRepository.existsByUsername(registerRequest.getUsername())
                .flatMap(usernameExists -> {
                    if (Boolean.TRUE.equals(usernameExists)) {
                        return Mono.error(new RuntimeException("Username already exists"));
                    }
                    return userRepository.existsByEmail(registerRequest.getEmail());
                })
                .flatMap(emailExists -> {
                    if (Boolean.TRUE.equals(emailExists)) {
                        return Mono.error(new RuntimeException("Email already exists"));
                    }

                    User newUser = User.builder()
                            .username(registerRequest.getUsername())
                            .email(registerRequest.getEmail())
                            .password(passwordEncoder.encode(registerRequest.getPassword()))
                            .roles(new ArrayList<>())
                            .build();

                    // Add default role
                    newUser.getRoles().add(User.Role.ROLE_USER.name());

                    return userRepository.save(newUser)
                            .onErrorStop();
                })
                .flatMap(user -> customAuthenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                registerRequest.getUsername(), registerRequest.getPassword())))
                .flatMap(this::generateAuthResponse)
                .onErrorResume(e -> {
                    log.error("Registration error: {}", e.getMessage(), e);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                });
    }

    private Mono<AuthDTO.AuthResponse> generateAuthResponse(Authentication authentication) {
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(user -> {
                    String token = jwtUtil.generateToken(
                            org.springframework.security.core.userdetails.User
                                    .withUsername(username)
                                    .password(user.getPassword())
                                    .authorities(user.getRoles().stream()
                                            .map(role -> new SimpleGrantedAuthority(role))
                                            .collect(Collectors.toList()))
                                    .build(),
                            user.getId()
                    );

                    return AuthDTO.AuthResponse.builder()
                            .token(token)
                            .id(user.getId().toHexString())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .roles(user.getRoles().stream()
                                    .collect(Collectors.toSet()))
                            .build();
                });
    }
}