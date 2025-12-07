package com.booking.booking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booking.booking.models.Users;

public interface UsersRepository extends JpaRepository<Users,Long>{
    
}
