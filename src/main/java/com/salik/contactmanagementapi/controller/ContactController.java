package com.salik.contactmanagementapi.controller;

import com.salik.contactmanagementapi.dto.ContactDTO;
import com.salik.contactmanagementapi.dto.PageResponse;
import com.salik.contactmanagementapi.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Contact Management API")
@SecurityRequirement(name = "bearerAuth")
public class ContactController extends BaseController{

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Create a new contact")
    public Mono<ResponseEntity<ContactDTO>> createContact(
            Authentication authentication,
            @Valid @RequestBody ContactDTO contactDTO) {

        return contactService.createContact(new ObjectId(extractUserId(authentication)), contactDTO)
                .map(createdContact -> ResponseEntity.status(HttpStatus.CREATED).body(createdContact))
                .onErrorResume(e -> {
                    if (e.getMessage().contains("Email already exists")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(contactDTO));
                    }
                    return Mono.error(e);
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID")
    public Mono<ResponseEntity<ContactDTO>> getContactById(
            Authentication authentication,
            @PathVariable String id) {

        return contactService.getContactById(new ObjectId(extractUserId(authentication)), new ObjectId(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing contact")
    public Mono<ResponseEntity<ContactDTO>> updateContact(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ContactDTO contactDTO) {

        return contactService.updateContact(new ObjectId(extractUserId(authentication)), new ObjectId(id), contactDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact")
    public Mono<ResponseEntity<Void>> deleteContact(
            Authentication authentication,
            @PathVariable String id) {

        return contactService.deleteContact(new ObjectId(extractUserId(authentication)), new ObjectId(id))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @GetMapping
    @Operation(summary = "Get all contacts with pagination and sorting")
    public Mono<ResponseEntity<PageResponse<ContactDTO>>> getAllContacts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return contactService.getContactsPage(
                        new ObjectId(extractUserId(authentication)), page, size, sortBy, direction)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/search")
    @Operation(summary = "Search contacts by keyword")
    public Flux<ContactDTO> searchContacts(
            Authentication authentication,
            @RequestParam(required = false) String query) {

        return contactService.searchContacts(new ObjectId(extractUserId(authentication)), query);
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get contacts by tag")
    public Flux<ContactDTO> getContactsByTag(
            Authentication authentication,
            @PathVariable String tag) {

        return contactService.getContactsByTag(new ObjectId(extractUserId(authentication)), tag);
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check if email exists")
    public Mono<ResponseEntity<Boolean>> checkEmailExists(
            Authentication authentication,
            @RequestParam String email) {

        return contactService.checkEmailExists(new ObjectId(extractUserId(authentication)), email)
                .map(ResponseEntity::ok);
    }
}