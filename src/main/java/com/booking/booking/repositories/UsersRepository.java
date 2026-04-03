package com.booking.booking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.models.Users;

public interface UsersRepository extends JpaRepository<Users,Long>{
    Optional<Users>findByEmail(String email);
    Optional<Users> findByKeycloakId(String keycloakId);

    @Query("SELECT u FROM Users u JOIN u.skills s WHERE s = :skill")
    List<Users> findUsersBySkill(@Param("skill") TechSkillsENUM skill);

    List<Users> findByRoles(RolesENUM role);
    
}
