package com.salik.contactmanagementapi.service;

import com.salik.contactmanagementapi.dto.UserDTO;
import com.salik.contactmanagementapi.model.User;
import com.salik.contactmanagementapi.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private ObjectId userId;
    private UserDTO.UserProfileUpdateRequest updateRequest;
    private UserDTO.PasswordChangeRequest passwordChangeRequest;

    @BeforeEach
    public void setup() {
        userId = new ObjectId();

        // Setup User
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");

        List<String> roles = new ArrayList<>();
        roles.add(User.Role.ROLE_USER.name());
        user.setRoles(roles);

        // Setup Update Request
        updateRequest = new UserDTO.UserProfileUpdateRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setEmail("jane.smith@example.com");

        // Setup Password Change Request
        passwordChangeRequest = new UserDTO.PasswordChangeRequest();
        passwordChangeRequest.setCurrentPassword("currentPassword");
        passwordChangeRequest.setNewPassword("newPassword");
        passwordChangeRequest.setConfirmPassword("newPassword");
    }

    @Test
    public void testGetUserProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        StepVerifier.create(userService.getUserProfile(userId))
                .expectNextMatches(profile ->
                        profile.getUsername().equals("testuser") &&
                                profile.getEmail().equals("test@example.com") &&
                                profile.getFirstName().equals("John") &&
                                profile.getLastName().equals("Doe")
                )
                .verifyComplete();
    }

    @Test
    public void testGetUserProfile_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserProfile(userId))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    public void testUpdateUserProfile_Success() {
        User updatedUser = user;
        updatedUser.setFirstName("Jane");
        updatedUser.setLastName("Smith");
        updatedUser.setEmail("jane.smith@example.com");

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        StepVerifier.create(userService.updateUserProfile(userId, updateRequest))
                .expectNextMatches(profile ->
                        profile.getFirstName().equals("Jane") &&
                                profile.getLastName().equals("Smith") &&
                                profile.getEmail().equals("jane.smith@example.com")
                )
                .verifyComplete();
    }

    @Test
    public void testUpdateUserProfile_EmailExists() {
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.existsByEmail(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(userService.updateUserProfile(userId, updateRequest))
                .expectErrorMatches(error ->
                        error instanceof ResponseStatusException &&
                                ((ResponseStatusException) error).getStatusCode() == HttpStatus.CONFLICT
                )
                .verify();
    }

    @Test
    public void testChangePassword_Success() {
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        User updatedUser = user;
        updatedUser.setPassword("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        StepVerifier.create(userService.changePassword(userId, passwordChangeRequest))
                .verifyComplete();
    }

    @Test
    public void testChangePassword_PasswordMismatch() {
        passwordChangeRequest.setConfirmPassword("differentPassword");

        StepVerifier.create(userService.changePassword(userId, passwordChangeRequest))
                .expectErrorMatches(error ->
                        error instanceof ResponseStatusException &&
                                ((ResponseStatusException) error).getStatusCode() == HttpStatus.BAD_REQUEST
                )
                .verify();
    }

    @Test
    public void testChangePassword_IncorrectCurrentPassword() {
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        StepVerifier.create(userService.changePassword(userId, passwordChangeRequest))
                .expectErrorMatches(error ->
                        error instanceof ResponseStatusException &&
                                ((ResponseStatusException) error).getStatusCode() == HttpStatus.BAD_REQUEST
                )
                .verify();
    }
}