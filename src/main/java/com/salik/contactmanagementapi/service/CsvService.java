package com.salik.contactmanagementapi.service;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.salik.contactmanagementapi.dto.ContactDTO;
import com.salik.contactmanagementapi.dto.CsvContactDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CsvService {

    public Flux<ContactDTO> parseCsvContacts(Flux<DataBuffer> dataBufferFlux) {
        return DataBufferUtils.join(dataBufferFlux)
                .flatMapMany(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return Mono.fromCallable(() -> {
                                try (CSVReader reader = new CSVReader(new InputStreamReader(
                                        new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {

                                    CsvToBean<CsvContactDTO> csvToBean = new CsvToBeanBuilder<CsvContactDTO>(reader)
                                            .withType(CsvContactDTO.class)
                                            .withIgnoreLeadingWhiteSpace(true)
                                            .build();

                                    List<CsvContactDTO> csvContacts = csvToBean.parse();
                                    List<ContactDTO> contacts = new ArrayList<>();

                                    for (CsvContactDTO csvContact : csvContacts) {
                                        contacts.add(csvContact.toContactDTO());
                                    }

                                    return contacts;
                                } catch (Exception e) {
                                    log.error("Error parsing CSV", e);
                                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Error parsing CSV: " + e.getMessage());
                                }
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMapMany(Flux::fromIterable);
                });
    }


    public Mono<DefaultDataBuffer> generateCsvFromContacts(List<ContactDTO> contacts) {
        return Mono.fromCallable(() -> {
            if (contacts.isEmpty()) {
                log.info("No contacts found to export, generating empty CSV with headers only");
            } else {
                log.info("Generating CSV for {} contacts", contacts.size());
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                // Write the header
                writer.write("First Name,Last Name,Email,Phone Number,Street,City,State,Zip Code,Country,Tags,Company,Job Title,Notes\n");

                // Write each contact
                for (ContactDTO contact : contacts) {
                    String firstName = contact.getFirstName() != null ? contact.getFirstName() : "";
                    String lastName = contact.getLastName() != null ? contact.getLastName() : "";
                    String email = contact.getEmail() != null ? contact.getEmail() : "";
                    String phoneNumber = contact.getPhoneNumber() != null ? contact.getPhoneNumber() : "";

                    // Address fields
                    String street = "", city = "", state = "", zipCode = "", country = "";
                    if (contact.getAddress() != null) {
                        street = contact.getAddress().getStreet() != null ? contact.getAddress().getStreet() : "";
                        city = contact.getAddress().getCity() != null ? contact.getAddress().getCity() : "";
                        state = contact.getAddress().getState() != null ? contact.getAddress().getState() : "";
                        zipCode = contact.getAddress().getZipCode() != null ? contact.getAddress().getZipCode() : "";
                        country = contact.getAddress().getCountry() != null ? contact.getAddress().getCountry() : "";
                    }

                    // Tags
                    String tags = "";
                    if (contact.getTags() != null && !contact.getTags().isEmpty()) {
                        tags = String.join(", ", contact.getTags());
                    }

                    String company = contact.getCompany() != null ? contact.getCompany() : "";
                    String jobTitle = contact.getJobTitle() != null ? contact.getJobTitle() : "";
                    String notes = contact.getNotes() != null ? contact.getNotes() : "";

                    // Build the CSV line
                    StringBuilder sb = new StringBuilder();
                    sb.append(escapeField(firstName)).append(",");
                    sb.append(escapeField(lastName)).append(",");
                    sb.append(escapeField(email)).append(",");
                    sb.append(escapeField(phoneNumber)).append(",");
                    sb.append(escapeField(street)).append(",");
                    sb.append(escapeField(city)).append(",");
                    sb.append(escapeField(state)).append(",");
                    sb.append(escapeField(zipCode)).append(",");
                    sb.append(escapeField(country)).append(",");
                    sb.append(escapeField(tags)).append(",");
                    sb.append(escapeField(company)).append(",");
                    sb.append(escapeField(jobTitle)).append(",");
                    sb.append(escapeField(notes));

                    writer.write(sb.toString() + "\n");
                }

                writer.flush();
            } catch (Exception e) {
                log.error("Error generating CSV", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error generating CSV: " + e.getMessage());
            }

            byte[] bytes = outputStream.toByteArray();
            log.info("Generated CSV with {} bytes", bytes.length);
            return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String escapeField(String field) {
        if (field == null) {
            return "";
        }

        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}