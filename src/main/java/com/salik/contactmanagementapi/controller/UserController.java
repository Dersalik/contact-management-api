package com.salik.contactmanagementapi.controller;

import com.salik.contactmanagementapi.dto.UserDTO;
import com.salik.contactmanagementapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User Profile Management API")
@SecurityRequirement(name = "bearerAuth")
public class UserController extends BaseController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile")
    public Mono<ResponseEntity<UserDTO.UserProfileResponse>> getUserProfile(Authentication authentication) {
        return userService.getUserProfile(new ObjectId(extractUserId(authentication)))
                .map(ResponseEntity::ok);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user's profile")
    public Mono<ResponseEntity<UserDTO.UserProfileResponse>> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UserDTO.UserProfileUpdateRequest updateRequest) {

        return userService.updateUserProfile(new ObjectId(extractUserId(authentication)), updateRequest)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change current user's password")
    public Mono<ResponseEntity<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody UserDTO.PasswordChangeRequest passwordRequest) {

        return userService.changePassword(new ObjectId(extractUserId(authentication)), passwordRequest)
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}