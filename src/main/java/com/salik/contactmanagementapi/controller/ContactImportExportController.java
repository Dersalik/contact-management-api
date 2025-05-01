package com.salik.contactmanagementapi.controller;

import com.salik.contactmanagementapi.dto.ContactDTO;
import com.salik.contactmanagementapi.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/contacts/import-export")
@RequiredArgsConstructor
@Tag(name = "Contact Import/Export", description = "Contact Import/Export Operations")
@SecurityRequirement(name = "bearerAuth")
public class ContactImportExportController extends BaseController {

    private final ContactService contactService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import contacts from CSV file")
    public Flux<ContactDTO> importContacts(
            Authentication authentication,
            @RequestPart("file") Mono<FilePart> filePart) {

        return filePart
                .flatMapMany(part -> {
                    String filename = part.filename();
                    if (!filename.toLowerCase().endsWith(".csv")) {
                        return Flux.error(new IllegalArgumentException("Only CSV files are supported"));
                    }
                    return contactService.importContactsFromCsv(
                            new ObjectId(extractUserId(authentication)),
                            part.content());
                });
    }

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Export contacts to CSV file")
    public Mono<ResponseEntity<byte[]>> exportContacts(Authentication authentication) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "contacts_" + timestamp + ".csv";

        return contactService.exportContactsToCsv(new ObjectId(extractUserId(authentication)))
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(bytes);
                });
    }
}