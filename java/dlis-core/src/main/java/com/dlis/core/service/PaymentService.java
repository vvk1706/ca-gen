package com.dlis.core.service;

import com.dlis.core.domain.Payment;

/**
 * Service interface for payment processing.
 * Derived from AB-PROCESS-PAYMENT.
 */
public interface PaymentService {

    /**
     * Calculate fee, record payment and advance application status to PA.
     *
     * @return ServiceResult carrying payment domain object (with generated receiptNumber)
     */
    ServiceResult<Payment> processPayment(Long applicationId,
                                          Long candidateId,
                                          String paymentMethod,
                                          String paymentReference,
                                          String processedBy);
}
