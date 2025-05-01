package com.salik.contactmanagementapi.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvContactDTO {

    @CsvBindByName(column = "First Name")
    private String firstName;

    @CsvBindByName(column = "Last Name")
    private String lastName;

    @CsvBindByName(column = "Email")
    private String email;

    @CsvBindByName(column = "Phone Number")
    private String phoneNumber;

    @CsvBindByName(column = "Street")
    private String street;

    @CsvBindByName(column = "City")
    private String city;

    @CsvBindByName(column = "State")
    private String state;

    @CsvBindByName(column = "Zip Code")
    private String zipCode;

    @CsvBindByName(column = "Country")
    private String country;

    @CsvBindByName(column = "Tags")
    private String tags;

    @CsvBindByName(column = "Company")
    private String company;

    @CsvBindByName(column = "Job Title")
    private String jobTitle;

    @CsvBindByName(column = "Notes")
    private String notes;

    public ContactDTO toContactDTO() {
        ContactDTO.AddressDTO addressDTO = null;

        if (street != null || city != null || state != null || zipCode != null || country != null) {
            addressDTO = ContactDTO.AddressDTO.builder()
                    .street(street)
                    .city(city)
                    .state(state)
                    .zipCode(zipCode)
                    .country(country)
                    .build();
        }

        Set<String> tagSet = new HashSet<>();
        if (tags != null && !tags.isEmpty()) {
            tagSet = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
        }

        return ContactDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .address(addressDTO)
                .tags(tagSet)
                .company(company)
                .jobTitle(jobTitle)
                .notes(notes)
                .build();
    }

    public static CsvContactDTO fromContactDTO(ContactDTO contactDTO) {
        CsvContactDTO csvContactDTO = new CsvContactDTO();
        csvContactDTO.setFirstName(contactDTO.getFirstName());
        csvContactDTO.setLastName(contactDTO.getLastName());
        csvContactDTO.setEmail(contactDTO.getEmail());
        csvContactDTO.setPhoneNumber(contactDTO.getPhoneNumber());
        csvContactDTO.setCompany(contactDTO.getCompany());
        csvContactDTO.setJobTitle(contactDTO.getJobTitle());
        csvContactDTO.setNotes(contactDTO.getNotes());

        if (contactDTO.getAddress() != null) {
            csvContactDTO.setStreet(contactDTO.getAddress().getStreet());
            csvContactDTO.setCity(contactDTO.getAddress().getCity());
            csvContactDTO.setState(contactDTO.getAddress().getState());
            csvContactDTO.setZipCode(contactDTO.getAddress().getZipCode());
            csvContactDTO.setCountry(contactDTO.getAddress().getCountry());
        }

        if (contactDTO.getTags() != null && !contactDTO.getTags().isEmpty()) {
            csvContactDTO.setTags(String.join(", ", contactDTO.getTags()));
        }

        return csvContactDTO;
    }
}