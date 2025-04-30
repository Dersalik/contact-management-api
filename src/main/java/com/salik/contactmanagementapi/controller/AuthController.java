package com.salik.contactmanagementapi.controller;

import com.salik.contactmanagementapi.dto.AuthDTO;
import com.salik.contactmanagementapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public Mono<ResponseEntity<AuthDTO.AuthResponse>> login(
            @Valid @RequestBody AuthDTO.LoginRequest loginRequest) {
        return authService.login(loginRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .build()));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public Mono<ResponseEntity<AuthDTO.AuthResponse>> register(
            @Valid @RequestBody AuthDTO.RegisterRequest registerRequest) {
        return authService.register(registerRequest)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .build()));
    }
}