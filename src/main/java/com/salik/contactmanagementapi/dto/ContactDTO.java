package com.salik.contactmanagementapi.dto;

import com.salik.contactmanagementapi.model.Contact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.bson.types.ObjectId;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {

    private String id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;

    private AddressDTO address;

    private String company;

    private String jobTitle;

    private String notes;

    private String profileImageUrl;

    private boolean favorite;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    public Contact toEntity() {
        Contact contact = new Contact();
        if (this.id != null && !this.id.isEmpty()) {
            contact.setId(new ObjectId(this.id));
        }
        contact.setFirstName(this.firstName);
        contact.setLastName(this.lastName);
        contact.setEmail(this.email);
        contact.setPhoneNumber(this.phoneNumber);
        contact.setTags(this.tags);
        contact.setCompany(this.company);
        contact.setJobTitle(this.jobTitle);
        contact.setNotes(this.notes);
        contact.setProfileImageUrl(this.profileImageUrl);
        contact.setFavorite(this.favorite);

        if (this.address != null) {
            Contact.Address contactAddress = new Contact.Address();
            contactAddress.setStreet(this.address.getStreet());
            contactAddress.setCity(this.address.getCity());
            contactAddress.setState(this.address.getState());
            contactAddress.setZipCode(this.address.getZipCode());
            contactAddress.setCountry(this.address.getCountry());
            contact.setAddress(contactAddress);
        }

        return contact;
    }

    public static ContactDTO fromEntity(Contact contact) {
        ContactDTO dto = new ContactDTO();
        dto.setId(contact.getId().toHexString());
        dto.setFirstName(contact.getFirstName());
        dto.setLastName(contact.getLastName());
        dto.setEmail(contact.getEmail());
        dto.setPhoneNumber(contact.getPhoneNumber());
        dto.setTags(contact.getTags());
        dto.setCompany(contact.getCompany());
        dto.setJobTitle(contact.getJobTitle());
        dto.setNotes(contact.getNotes());
        dto.setProfileImageUrl(contact.getProfileImageUrl());
        dto.setFavorite(contact.isFavorite());

        if (contact.getAddress() != null) {
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setStreet(contact.getAddress().getStreet());
            addressDTO.setCity(contact.getAddress().getCity());
            addressDTO.setState(contact.getAddress().getState());
            addressDTO.setZipCode(contact.getAddress().getZipCode());
            addressDTO.setCountry(contact.getAddress().getCountry());
            dto.setAddress(addressDTO);
        }

        return dto;
    }
}