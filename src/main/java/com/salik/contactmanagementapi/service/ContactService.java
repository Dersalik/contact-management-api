package com.salik.contactmanagementapi.service;

import com.salik.contactmanagementapi.dto.ContactDTO;
import com.salik.contactmanagementapi.dto.PageResponse;
import com.salik.contactmanagementapi.model.Contact;
import com.salik.contactmanagementapi.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    public Mono<ContactDTO> createContact(ObjectId userId, ContactDTO contactDTO) {
        Contact contact = contactDTO.toEntity();
        contact.setUserId(userId);

        return contactRepository.save(contact)
                .map(ContactDTO::fromEntity);
    }

    public Mono<ContactDTO> getContactById(ObjectId userId, ObjectId contactId) {
        return contactRepository.findById(contactId)
                .filter(contact -> contact.getUserId().equals(userId))
                .map(ContactDTO::fromEntity)
                .switchIfEmpty(Mono.error(new RuntimeException("Contact not found or not authorized")));
    }

    public Mono<ContactDTO> updateContact(ObjectId userId, ObjectId contactId, ContactDTO contactDTO) {
        return contactRepository.findById(contactId)
                .filter(contact -> contact.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Contact not found or not authorized")))
                .flatMap(contact -> {
                    Contact updatedContact = contactDTO.toEntity();
                    updatedContact.setId(contact.getId());
                    updatedContact.setUserId(userId);
                    updatedContact.setCreatedAt(contact.getCreatedAt());
                    return contactRepository.save(updatedContact);
                })
                .map(ContactDTO::fromEntity);
    }

    public Mono<Void> deleteContact(ObjectId userId, ObjectId contactId) {
        return contactRepository.findById(contactId)
                .filter(contact -> contact.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Contact not found or not authorized")))
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
}
