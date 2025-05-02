package com.salik.contactmanagementapi.service;

import com.salik.contactmanagementapi.dto.ContactDTO;
import com.salik.contactmanagementapi.model.Contact;
import com.salik.contactmanagementapi.repository.ContactRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CsvService csvService;

    @InjectMocks
    private ContactService contactService;

    private ObjectId userId;
    private ObjectId contactId;
    private Contact contact;
    private ContactDTO contactDTO;

    @BeforeEach
    public void setup() {
        userId = new ObjectId();
        contactId = new ObjectId();

        // Setup contact entity
        contact = new Contact();
        contact.setId(contactId);
        contact.setFirstName("John");
        contact.setLastName("Doe");
        contact.setEmail("john.doe@example.com");
        contact.setPhoneNumber("+1234567890");
        contact.setUserId(userId);
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());

        Set<String> tags = new HashSet<>();
        tags.add("friend");
        tags.add("work");
        contact.setTags(tags);

        Contact.Address address = new Contact.Address();
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setZipCode("10001");
        address.setCountry("USA");
        contact.setAddress(address);

        // Setup contact DTO
        contactDTO = ContactDTO.fromEntity(contact);
    }

    @Test
    public void testGetContactById_Success() {
        when(contactRepository.findById(contactId)).thenReturn(Mono.just(contact));

        StepVerifier.create(contactService.getContactById(userId, contactId))
                .expectNextMatches(dto ->
                        dto.getFirstName().equals("John") &&
                                dto.getLastName().equals("Doe") &&
                                dto.getEmail().equals("john.doe@example.com")
                )
                .verifyComplete();
    }

    @Test
    public void testGetContactById_NotFound() {
        when(contactRepository.findById(contactId)).thenReturn(Mono.empty());

        StepVerifier.create(contactService.getContactById(userId, contactId))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    public void testGetContactById_WrongUser() {
        ObjectId differentUserId = new ObjectId();
        when(contactRepository.findById(contactId)).thenReturn(Mono.just(contact));

        StepVerifier.create(contactService.getContactById(differentUserId, contactId))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    public void testCreateContact_Success() {
        when(contactRepository.findByUserIdAndEmail(any(), any())).thenReturn(Mono.empty());
        when(contactRepository.save(any(Contact.class))).thenReturn(Mono.just(contact));

        StepVerifier.create(contactService.createContact(userId, contactDTO))
                .expectNextMatches(dto ->
                        dto.getFirstName().equals("John") &&
                                dto.getLastName().equals("Doe") &&
                                dto.getEmail().equals("john.doe@example.com")
                )
                .verifyComplete();
    }

    @Test
    public void testCreateContact_DuplicateEmail() {
        when(contactRepository.findByUserIdAndEmail(any(), any())).thenReturn(Mono.just(contact));

        StepVerifier.create(contactService.createContact(userId, contactDTO))
                .expectErrorMatches(error ->
                        error instanceof ResponseStatusException &&
                                ((ResponseStatusException) error).getStatusCode() == HttpStatus.CONFLICT
                )
                .verify();
    }

    @Test
    public void testUpdateContact_Success() {
        ContactDTO updatedDTO = ContactDTO.builder()
                .id(contactId.toHexString())
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .build();

        Contact updatedContact = contact;
        updatedContact.setFirstName("Jane");
        updatedContact.setEmail("jane.doe@example.com");

        when(contactRepository.findById(contactId)).thenReturn(Mono.just(contact));
        when(contactRepository.findByUserIdAndEmail(any(), any())).thenReturn(Mono.empty());
        when(contactRepository.save(any(Contact.class))).thenReturn(Mono.just(updatedContact));

        StepVerifier.create(contactService.updateContact(userId, contactId, updatedDTO))
                .expectNextMatches(dto ->
                        dto.getFirstName().equals("Jane") &&
                                dto.getLastName().equals("Doe") &&
                                dto.getEmail().equals("jane.doe@example.com")
                )
                .verifyComplete();
    }

    @Test
    public void testDeleteContact_Success() {
        when(contactRepository.findById(contactId)).thenReturn(Mono.just(contact));
        when(contactRepository.delete(contact)).thenReturn(Mono.empty());

        StepVerifier.create(contactService.deleteContact(userId, contactId))
                .verifyComplete();
    }

    @Test
    public void testGetAllContacts() {
        Contact contact2 = new Contact();
        contact2.setId(new ObjectId());
        contact2.setFirstName("Jane");
        contact2.setLastName("Smith");
        contact2.setEmail("jane.smith@example.com");
        contact2.setUserId(userId);

        when(contactRepository.findByUserId(userId)).thenReturn(Flux.just(contact, contact2));

        StepVerifier.create(contactService.getAllContacts(userId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void testSearchContacts() {
        String query = "John";
        when(contactRepository.searchByUserIdAndKeyword(userId, query)).thenReturn(Flux.just(contact));

        StepVerifier.create(contactService.searchContacts(userId, query))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void testGetContactsByTag() {
        String tag = "friend";
        when(contactRepository.findByUserIdAndTagsContaining(userId, tag)).thenReturn(Flux.just(contact));

        StepVerifier.create(contactService.getContactsByTag(userId, tag))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void testGetContactsPage() {
        int page = 0;
        int size = 10;
        String sortBy = "firstName";
        String direction = "asc";

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC, sortBy);

        when(contactRepository.findByUserId(userId, pageRequest)).thenReturn(Flux.just(contact));
        when(contactRepository.countByUserId(userId)).thenReturn(Mono.just(1L));

        StepVerifier.create(contactService.getContactsPage(userId, page, size, sortBy, direction))
                .expectNextMatches(pageResponse ->
                        pageResponse.getTotalElements() == 1 &&
                                pageResponse.getTotalPages() == 1 &&
                                pageResponse.getContent().size() == 1 &&
                                pageResponse.getContent().get(0).getFirstName().equals("John")
                )
                .verifyComplete();
    }

    @Test
    public void testCheckEmailExists_True() {
        String email = "john.doe@example.com";
        when(contactRepository.findByUserIdAndEmail(userId, email)).thenReturn(Mono.just(contact));

        StepVerifier.create(contactService.checkEmailExists(userId, email))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testCheckEmailExists_False() {
        String email = "nonexistent@example.com";
        when(contactRepository.findByUserIdAndEmail(userId, email)).thenReturn(Mono.empty());

        StepVerifier.create(contactService.checkEmailExists(userId, email))
                .expectNext(false)
                .verifyComplete();
    }
}