package com.mapdrop.repository;

import com.mapdrop.models.ERole;
import com.mapdrop.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole role);
}
