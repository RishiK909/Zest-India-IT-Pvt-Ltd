package com.zest.products.repository;

import com.zest.products.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);

    boolean existsByUserName(String userName);

    Optional<Users> findByPhoneNumber(String phoneNumber);
}
