package com.booking.booking.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.Users;

public interface UsersRepository extends JpaRepository<Users,Long>{
    Optional<Users>findByEmail(String email);
    Optional<Users> findByKeycloakId(String keycloakId);
}
