package com.glowvera.repository;

import com.glowvera.entity.Concern;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcernRepository extends JpaRepository<Concern, Long> {

    Optional<Concern> findByName(String name);
}
