package com.pfh.user.entity;

import com.pfh.user.enums.UserRole;
import com.pfh.user.enums.UserStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @PrePersist
    private void defaultData() {
        if (role == null) {
            role = UserRole.NORMAL_USER;
        }

        if (status == null) {
            status = UserStatus.ACTIVE; // Active by default
        }
    }
}
