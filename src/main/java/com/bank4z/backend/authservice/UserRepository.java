package com.bank4z.backend.authservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    boolean existsByIdNumber(String idNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByEmail(String email);
}