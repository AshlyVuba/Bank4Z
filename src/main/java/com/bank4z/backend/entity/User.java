package com.bank4z.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseAuditableEntity {

    public enum Role { USER, ADMIN }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "id_number", nullable = false, unique = true, length = 20)
    private String idNumber;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    protected User() {
        // required by JPA
    }

    public User(String fullName, String email, String idNumber, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.idNumber = idNumber;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getIdNumber() { return idNumber; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(Role role) { this.role = role; }
}