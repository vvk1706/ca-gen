package com.dlis.core.service;

import com.dlis.core.domain.IssuedLicense;

/**
 * Service interface for dual-authority approval workflow and license issuance.
 * Derived from AB-RECORD-APPROVAL-1, AB-RECORD-APPROVAL-2, AB-ISSUE-LICENSE.
 */
public interface ApprovalService {

    /** AB-RECORD-APPROVAL-1 */
    ServiceResult<Void> recordApproval1(Long applicationId,
                                        String authorityUserCode,
                                        String decision,
                                        String decisionNotes);

    /** AB-RECORD-APPROVAL-2 */
    ServiceResult<Void> recordApproval2(Long applicationId,
                                        String authorityUserCode,
                                        String decision,
                                        String decisionNotes);

    /** AB-ISSUE-LICENSE */
    ServiceResult<IssuedLicense> issueLicense(Long applicationId,
                                              String vehicleClass,
                                              String restrictions,
                                              String issuedByOfficer,
                                              String issuedByAuthority);
}
