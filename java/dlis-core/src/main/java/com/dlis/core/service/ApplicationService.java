package com.dlis.core.service;

import com.dlis.core.domain.LicenseApplication;

/**
 * Service interface for license application workflow.
 * Each method maps to one CA Gen action block.
 */
public interface ApplicationService {

    /** AB-CREATE-APPLICATION */
    ServiceResult<Long> createApplication(Long candidateId, String licenseType, String createdBy);

    /** AB-CHECK-ELIGIBILITY */
    ServiceResult<String> checkEligibility(Long applicationId, String checkedBy);

    /** AB-CHECK-HISTORY */
    ServiceResult<String> checkHistory(Long applicationId, String checkedBy);

    /** AB-INQUIRE-APPLICATION-STATUS */
    ServiceResult<LicenseApplication> inquireStatus(Long applicationId);
}
