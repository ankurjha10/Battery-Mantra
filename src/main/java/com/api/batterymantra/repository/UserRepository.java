package com.api.batterymantra.repository;

import com.api.batterymantra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findAllByUsername(String username);
    List<User> findAllByEmail(String email);
    List<User> findAllByPhoneNumber(String phoneNumber);

    Optional<User> findFirstByUsername(String username);
    Optional<User> findFirstByEmail(String email);
    Optional<User> findFirstByPhoneNumber(String phoneNumber);
    Optional<User> findFirstByPhoneNumberOrderByCreatedAtAsc(String phoneNumber);

    default User findByUsername(String username) {
        return findFirstByUsername(username).orElse(null);
    }

    default User findByEmail(String email) {
        return findFirstByEmail(email).orElse(null);
    }

    default User findByPhoneNumber(String phoneNumber) {
        return findFirstByPhoneNumberOrderByCreatedAtAsc(phoneNumber)
                .orElseGet(() -> findFirstByPhoneNumber(phoneNumber).orElse(null));
    }

    boolean existsByEmail(String email);
    boolean existsByEmailOrPhoneNumber(String email, String phoneNumber);
}