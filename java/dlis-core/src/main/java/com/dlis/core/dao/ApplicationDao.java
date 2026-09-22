package com.dlis.core.dao;

import com.dlis.core.domain.LicenseApplication;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for DLIS.LICENSE_APPLICATION table.
 */
public interface ApplicationDao {

    void insert(LicenseApplication app);

    Optional<LicenseApplication> findById(Long applicationId);

    /** Active (non-terminal) applications for a candidate and license type. */
    List<LicenseApplication> findActiveByCandidate(Long candidateId, String licenseType);

    void updateEligibilityCheck(Long applicationId,
                                String status,
                                java.time.LocalDate checkDate,
                                String notes,
                                String appStatus,
                                java.time.LocalDate lastUpdatedDate,
                                String lastUpdatedBy);

    void updateHistoryCheck(Long applicationId,
                            String status,
                            java.time.LocalDate checkDate,
                            String notes,
                            String appStatus,
                            java.time.LocalDate lastUpdatedDate,
                            String lastUpdatedBy);

    void updatePayment(Long applicationId,
                       String paymentStatus,
                       String paymentReference,
                       String appStatus,
                       java.time.LocalDate lastUpdatedDate,
                       String lastUpdatedBy);

    void updateApproval1(Long applicationId,
                         String decision,
                         String authorityName,
                         java.time.LocalDate decisionDate,
                         String notes,
                         String appStatus,
                         java.time.LocalDate lastUpdatedDate,
                         String lastUpdatedBy);

    void updateApproval2(Long applicationId,
                         String decision,
                         String authorityName,
                         java.time.LocalDate decisionDate,
                         String notes,
                         String appStatus,
                         java.time.LocalDate lastUpdatedDate,
                         String lastUpdatedBy);

    void updateApplicationStatus(Long applicationId,
                                 String appStatus,
                                 java.time.LocalDate lastUpdatedDate,
                                 String lastUpdatedBy);
}
