package com.salik.contactmanagementapi.service;

import com.salik.contactmanagementapi.dto.UserDTO;
import com.salik.contactmanagementapi.model.User;
import com.salik.contactmanagementapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<UserDTO.UserProfileResponse> getUserProfile(ObjectId userId) {
        return userRepository.findById(userId)
                .map(UserDTO::fromEntity)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found")));
    }

    public Mono<UserDTO.UserProfileResponse> updateUserProfile(ObjectId userId,
                                                               UserDTO.UserProfileUpdateRequest updateRequest) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(user -> {
                    if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
                        return userRepository.existsByEmail(updateRequest.getEmail())
                                .flatMap(exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        return Mono.error(new ResponseStatusException(
                                                HttpStatus.CONFLICT, "Email already exists"));
                                    }
                                    user.setEmail(updateRequest.getEmail());
                                    return Mono.just(user);
                                });
                    }
                    return Mono.just(user);
                })
                .flatMap(user -> {
                    if (updateRequest.getFirstName() != null) {
                        user.setFirstName(updateRequest.getFirstName());
                    }
                    if (updateRequest.getLastName() != null) {
                        user.setLastName(updateRequest.getLastName());
                    }
                    return userRepository.save(user);
                })
                .map(UserDTO::fromEntity);
    }

    public Mono<Void> changePassword(ObjectId userId, UserDTO.PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "New password and confirm password do not match"));
        }

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Current password is incorrect"));
                    }

                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    return userRepository.save(user);
                })
                .then();
    }
}