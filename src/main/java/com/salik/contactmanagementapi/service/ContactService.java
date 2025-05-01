package com.salik.contactmanagementapi.service;

import com.salik.contactmanagementapi.dto.ContactDTO;
import com.salik.contactmanagementapi.dto.PageResponse;
import com.salik.contactmanagementapi.model.Contact;
import com.salik.contactmanagementapi.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final CsvService csvService;

    public Mono<ContactDTO> createContact(ObjectId userId, ContactDTO contactDTO) {
        return checkDuplicateEmail(userId, contactDTO.getEmail(), null)
                .flatMap(isDuplicate -> {
                    if (Boolean.TRUE.equals(isDuplicate)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Email already exists"));
                    }

                    Contact contact = contactDTO.toEntity();
                    contact.setUserId(userId);
                    return contactRepository.save(contact);
                })
                .map(ContactDTO::fromEntity);
    }

    public Mono<ContactDTO> getContactById(ObjectId userId, ObjectId contactId) {
        return contactRepository.findById(contactId)
                .filter(contact -> contact.getUserId().equals(userId))
                .map(ContactDTO::fromEntity)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Contact not found or not authorized")));
    }

    public Mono<ContactDTO> updateContact(ObjectId userId, ObjectId contactId, ContactDTO contactDTO) {
        return contactRepository.findById(contactId)
                .filter(contact -> contact.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Contact not found or not authorized")))
                .flatMap(contact -> checkDuplicateEmail(userId, contactDTO.getEmail(), contactId)
                        .flatMap(isDuplicate -> {
                            if (Boolean.TRUE.equals(isDuplicate)) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.CONFLICT, "Email already exists"));
                            }

                            Contact updatedContact = contactDTO.toEntity();
                            updatedContact.setId(contact.getId());
                            updatedContact.setUserId(userId);
                            updatedContact.setCreatedAt(contact.getCreatedAt());
                            return contactRepository.save(updatedContact);
                        }))
                .map(ContactDTO::fromEntity);
    }

    public Mono<Void> deleteContact(ObjectId userId, ObjectId contactId) {
        return contactRepository.findById(contactId)
                .filter(contact -> contact.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Contact not found or not authorized")))
                .flatMap(contact -> contactRepository.delete(contact));
    }

    public Flux<ContactDTO> getAllContacts(ObjectId userId) {
        return contactRepository.findByUserId(userId)
                .map(ContactDTO::fromEntity);
    }

    public Mono<PageResponse<ContactDTO>> getContactsPage(ObjectId userId,
                                                          int page,
                                                          int size,
                                                          String sortBy,
                                                          String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, sortDirection, sortBy);
        return contactRepository.findByUserId(userId, pageRequest)
                .map(ContactDTO::fromEntity)
                .collectList()
                .zipWith(contactRepository.countByUserId(userId))
                .map(tuple -> {
                    var contacts = tuple.getT1();
                    var totalCount = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalCount / size);

                    return PageResponse.<ContactDTO>builder()
                            .content(contacts)
                            .totalElements(totalCount)
                            .totalPages(totalPages)
                            .currentPage(page)
                            .pageSize(size)
                            .first(page == 0)
                            .last(page == totalPages - 1 || totalPages == 0)
                            .build();
                });
    }

    public Flux<ContactDTO> searchContacts(ObjectId userId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllContacts(userId);
        }
        return contactRepository.searchByUserIdAndKeyword(userId, query)
                .map(ContactDTO::fromEntity);
    }

    public Flux<ContactDTO> getContactsByTag(ObjectId userId, String tag) {
        return contactRepository.findByUserIdAndTagsContaining(userId, tag)
                .map(ContactDTO::fromEntity);
    }

    public Mono<Boolean> checkEmailExists(ObjectId userId, String email) {
        return contactRepository.findByUserIdAndEmail(userId, email)
                .map(contact -> true)
                .defaultIfEmpty(false);
    }

    private Mono<Boolean> checkDuplicateEmail(ObjectId userId, String email, ObjectId excludeContactId) {
        if (email == null || email.isEmpty()) {
            return Mono.just(false);
        }

        return contactRepository.findByUserIdAndEmail(userId, email)
                .map(contact -> {
                    if (excludeContactId != null) {
                        return !contact.getId().equals(excludeContactId);
                    }
                    return true;
                })
                .defaultIfEmpty(false);
    }


    public Flux<ContactDTO> importContactsFromCsv(ObjectId userId, Flux<DataBuffer> fileContent) {
        return csvService.parseCsvContacts(fileContent)
                .flatMap(contactDTO -> {
                    Contact contact = contactDTO.toEntity();
                    contact.setUserId(userId);
                    return contactRepository.save(contact)
                            .map(ContactDTO::fromEntity)
                            .onErrorResume(e -> Mono.empty());
                });
    }

    public Mono<DefaultDataBuffer> exportContactsToCsv(ObjectId userId) {
        return contactRepository.findByUserId(userId)
                .map(ContactDTO::fromEntity)
                .collectList()
                .doOnNext(contacts -> {
                    if (contacts.isEmpty()) {
                        System.out.println("No contacts found for user " + userId);
                    } else {
                        System.out.println("Found " + contacts.size() + " contacts for export");
                    }
                })
                .flatMap(csvService::generateCsvFromContacts)
                .switchIfEmpty(Mono.defer(() -> {
                    // If we get here, it means no contacts were found, but we still want to return a CSV with headers
                    System.out.println("No contacts found, returning empty CSV with headers");
                    return csvService.generateCsvFromContacts(java.util.Collections.emptyList());
                }));
    }
}