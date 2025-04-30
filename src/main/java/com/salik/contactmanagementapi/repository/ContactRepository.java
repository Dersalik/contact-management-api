package com.salik.contactmanagementapi.repository;

import com.salik.contactmanagementapi.model.Contact;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ContactRepository extends ReactiveMongoRepository<Contact, ObjectId> {

    Flux<Contact> findByUserId(ObjectId userId);

    @Query("{ 'userId': ?0, $or: [ " +
            "{ 'firstName': { $regex: ?1, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?1, $options: 'i' } }, " +
            "{ 'email': { $regex: ?1, $options: 'i' } }, " +
            "{ 'phoneNumber': { $regex: ?1, $options: 'i' } } ] }")
    Flux<Contact> searchByUserIdAndKeyword(ObjectId userId, String keyword);

    Flux<Contact> findByUserIdAndTagsContaining(ObjectId userId, String tag);

    Mono<Long> countByUserId(ObjectId userId);

    // Pagination support
    Flux<Contact> findByUserId(ObjectId userId, Pageable pageable);

    Mono<Contact> findByUserIdAndEmail(ObjectId userId, String email);
}