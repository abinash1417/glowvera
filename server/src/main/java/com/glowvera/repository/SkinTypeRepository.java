package com.glowvera.repository;

import com.glowvera.entity.SkinType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkinTypeRepository extends JpaRepository<SkinType, Long> {

    Optional<SkinType> findByName(String name);
}
