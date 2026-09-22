package com.dlis.core.dao;

import com.dlis.core.domain.Payment;
import java.util.Optional;

/**
 * DAO interface for DLIS.PAYMENT table.
 */
public interface PaymentDao {

    void insert(Payment payment);

    Optional<Payment> findById(Long paymentId);
}
