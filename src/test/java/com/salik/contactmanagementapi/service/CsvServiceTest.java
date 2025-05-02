package com.salik.contactmanagementapi.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.salik.contactmanagementapi.dto.ContactDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvServiceTest {

    private CsvService csvService;
    private List<ContactDTO> testContacts;

    @BeforeEach
    void setUp() {
        csvService = new CsvService();
        testContacts = createTestContacts();
    }

    @Test
    void testGenerateCsvFromContacts() {
        Mono<DefaultDataBuffer> csvDataMono = csvService.generateCsvFromContacts(testContacts);

        StepVerifier.create(csvDataMono)
                .expectNextMatches(dataBuffer -> {
                    byte[] data = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(data);
                    String csvContent = new String(data, StandardCharsets.UTF_8);

                    // Verify CSV header is present
                    assertTrue(csvContent.startsWith("First Name,Last Name,Email,Phone Number,Street,City,State,Zip Code,Country,Tags,Company,Job Title,Notes"));

                    // Verify all contacts are in the CSV
                    for (ContactDTO contact : testContacts) {
                        assertTrue(csvContent.contains(contact.getFirstName() + "," + contact.getLastName()));
                        assertTrue(csvContent.contains(contact.getEmail()));
                    }

                    // Count the number of lines (header + all contacts)
                    String[] lines = csvContent.split("\n");
                    assertEquals(testContacts.size() + 1, lines.length);

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testGenerateCsvFromEmptyContacts() {
        Mono<DefaultDataBuffer> csvDataMono = csvService.generateCsvFromContacts(new ArrayList<>());

        StepVerifier.create(csvDataMono)
                .expectNextMatches(dataBuffer -> {
                    byte[] data = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(data);
                    String csvContent = new String(data, StandardCharsets.UTF_8);

                    // Verify CSV only contains header
                    assertTrue(csvContent.startsWith("First Name,Last Name,Email,Phone Number,Street,City,State,Zip Code,Country,Tags,Company,Job Title,Notes"));

                    // Count the number of lines (should be just the header)
                    String[] lines = csvContent.split("\n");
                    assertEquals(1, lines.length);

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testParseCsvContacts() {
        // Create a CSV string
        String csvContent = "First Name,Last Name,Email,Phone Number,Street,City,State,Zip Code,Country,Tags,Company,Job Title,Notes\n" +
                "Alice,Johnson,alice.j@example.com,+1-555-123-4567,123 Main St,New York,NY,10001,USA,\"friend, work\",Acme Inc,Developer,Alice is a great developer\n" +
                "Bob,Williams,bob.w@example.com,+1-555-987-6543,456 Park Ave,Boston,MA,02115,USA,family,Tech Co,Manager,Bob is a family friend";

        // Convert to DataBuffer
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buffer = factory.wrap(csvContent.getBytes(StandardCharsets.UTF_8));

        // Parse CSV
        Flux<ContactDTO> contactsFlux = csvService.parseCsvContacts(Flux.just(buffer));

        StepVerifier.create(contactsFlux)
                .expectNextMatches(contact ->
                        "Alice".equals(contact.getFirstName()) &&
                                "Johnson".equals(contact.getLastName()) &&
                                "alice.j@example.com".equals(contact.getEmail()) &&
                                contact.getTags().contains("friend") &&
                                contact.getTags().contains("work") &&
                                "123 Main St".equals(contact.getAddress().getStreet())
                )
                .expectNextMatches(contact ->
                        "Bob".equals(contact.getFirstName()) &&
                                "Williams".equals(contact.getLastName()) &&
                                "bob.w@example.com".equals(contact.getEmail()) &&
                                contact.getTags().contains("family") &&
                                "456 Park Ave".equals(contact.getAddress().getStreet())
                )
                .verifyComplete();
    }

    @Test
    void testParseCsvContacts_WithSpecialCharacters() {
        // Create a CSV string with special characters and quotes
        String csvContent = "First Name,Last Name,Email,Phone Number,Street,City,State,Zip Code,Country,Tags,Company,Job Title,Notes\n" +
                "John,\"Doe, Jr.\",john@example.com,+1-555-123-4567,\"123 Main St, Apt 4B\",New York,NY,10001,USA,\"friend, VIP\",\"Acme, Inc\",\"Developer, Senior\",\"John has, multiple, commas in notes\"";

        // Convert to DataBuffer
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buffer = factory.wrap(csvContent.getBytes(StandardCharsets.UTF_8));

        // Parse CSV
        Flux<ContactDTO> contactsFlux = csvService.parseCsvContacts(Flux.just(buffer));

        StepVerifier.create(contactsFlux)
                .expectNextMatches(contact ->
                        "John".equals(contact.getFirstName()) &&
                                "Doe, Jr.".equals(contact.getLastName()) &&
                                "john@example.com".equals(contact.getEmail()) &&
                                contact.getTags().contains("friend") &&
                                contact.getTags().contains("VIP") &&
                                "123 Main St, Apt 4B".equals(contact.getAddress().getStreet()) &&
                                "Acme, Inc".equals(contact.getCompany()) &&
                                "Developer, Senior".equals(contact.getJobTitle())
                )
                .verifyComplete();
    }

    @Test
    void testRoundTrip_GenerateAndParse() {
        // Generate CSV
        Mono<DefaultDataBuffer> csvDataMono = csvService.generateCsvFromContacts(testContacts);

        // Then parse it back
        StepVerifier.create(csvDataMono)
                .consumeNextWith(dataBuffer -> {
                    // Create a flux of this data buffer
                    Flux<DataBuffer> dataBufferFlux = Flux.just(dataBuffer);

                    // Parse the CSV content
                    Flux<ContactDTO> parsedContactsFlux = csvService.parseCsvContacts(dataBufferFlux);

                    // Collect all parsed contacts and verify
                    List<ContactDTO> parsedContacts = parsedContactsFlux.collectList().block();

                    assertNotNull(parsedContacts);
                    assertEquals(testContacts.size(), parsedContacts.size());

                    // Verify specific contact details were preserved
                    for (int i = 0; i < testContacts.size(); i++) {
                        ContactDTO original = testContacts.get(i);
                        ContactDTO parsed = parsedContacts.get(i);

                        assertEquals(original.getFirstName(), parsed.getFirstName());
                        assertEquals(original.getLastName(), parsed.getLastName());
                        assertEquals(original.getEmail(), parsed.getEmail());

                        if (original.getAddress() != null) {
                            assertNotNull(parsed.getAddress());
                            assertEquals(original.getAddress().getStreet(), parsed.getAddress().getStreet());
                            assertEquals(original.getAddress().getCity(), parsed.getAddress().getCity());
                        }

                        assertEquals(original.getTags().size(), parsed.getTags().size());
                    }
                })
                .verifyComplete();
    }

    @Test
    void testParseCsvContacts_EmptyFile() {
        // Create an empty CSV (just header)
        String csvContent = "First Name,Last Name,Email,Phone Number,Street,City,State,Zip Code,Country,Tags,Company,Job Title,Notes\n";

        // Convert to DataBuffer
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buffer = factory.wrap(csvContent.getBytes(StandardCharsets.UTF_8));

        // Parse CSV
        Flux<ContactDTO> contactsFlux = csvService.parseCsvContacts(Flux.just(buffer));

        // Should be empty since there's only a header
        StepVerifier.create(contactsFlux)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testEscapeField() {


        String field1 = "Simple text";
        String field2 = "Text with, comma";
        String field3 = "Text with \"quotes\"";
        String field4 = "Text with\nnewline";

        ContactDTO contact = new ContactDTO();
        contact.setFirstName(field1);
        contact.setLastName(field2);
        contact.setEmail(field3);
        contact.setPhoneNumber(field4);

        List<ContactDTO> contacts = List.of(contact);

        Mono<DefaultDataBuffer> csvDataMono = csvService.generateCsvFromContacts(contacts);

        StepVerifier.create(csvDataMono)
                .expectNextMatches(dataBuffer -> {
                    byte[] data = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(data);
                    String csvContent = new String(data, StandardCharsets.UTF_8);

                    // Check if fields are properly escaped
                    assertTrue(csvContent.contains(field1)); // No need to escape
                    assertTrue(csvContent.contains("\"" + field2 + "\"")); // Comma should be escaped
                    assertTrue(csvContent.contains("\"" + field3.replace("\"", "\"\"") + "\"")); // Quotes should be escaped

                    return true;
                })
                .verifyComplete();
    }

    private List<ContactDTO> createTestContacts() {
        List<ContactDTO> contacts = new ArrayList<>();

        // Contact 1
        ContactDTO contact1 = new ContactDTO();
        contact1.setFirstName("John");
        contact1.setLastName("Doe");
        contact1.setEmail("john.doe@example.com");
        contact1.setPhoneNumber("+1234567890");

        ContactDTO.AddressDTO address1 = new ContactDTO.AddressDTO();
        address1.setStreet("123 Main St");
        address1.setCity("New York");
        address1.setState("NY");
        address1.setZipCode("10001");
        address1.setCountry("USA");
        contact1.setAddress(address1);

        Set<String> tags1 = new HashSet<>();
        tags1.add("friend");
        tags1.add("work");
        contact1.setTags(tags1);

        contact1.setCompany("Acme Inc");
        contact1.setJobTitle("Developer");
        contact1.setNotes("John is a developer");

        // Contact 2
        ContactDTO contact2 = new ContactDTO();
        contact2.setFirstName("Jane");
        contact2.setLastName("Smith");
        contact2.setEmail("jane.smith@example.com");
        contact2.setPhoneNumber("+1987654321");

        ContactDTO.AddressDTO address2 = new ContactDTO.AddressDTO();
        address2.setStreet("456 Park Ave");
        address2.setCity("Boston");
        address2.setState("MA");
        address2.setZipCode("02115");
        address2.setCountry("USA");
        contact2.setAddress(address2);

        Set<String> tags2 = new HashSet<>();
        tags2.add("family");
        contact2.setTags(tags2);

        contact2.setCompany("Tech Co");
        contact2.setJobTitle("Manager");
        contact2.setNotes("Jane is a family friend");

        contacts.add(contact1);
        contacts.add(contact2);

        return contacts;
    }
}