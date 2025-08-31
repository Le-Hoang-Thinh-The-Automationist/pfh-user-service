package com.pfh.user.repository;

import com.pfh.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email); 
    boolean existsByEmailIgnoreCase(String email);    
}
