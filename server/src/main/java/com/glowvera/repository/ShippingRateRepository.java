package com.glowvera.repository;

import com.glowvera.entity.ShippingRate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

    Optional<ShippingRate> findByDistrict(String district);

    List<ShippingRate> findAllByOrderByDistrictAsc();
}
