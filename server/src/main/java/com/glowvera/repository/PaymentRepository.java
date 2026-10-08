package com.glowvera.repository;

import com.glowvera.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPayherePaymentId(String payherePaymentId);
}
