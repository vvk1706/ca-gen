package com.dlis.core.dao;

import com.dlis.core.domain.LicenseFeeSchedule;
import java.util.Optional;

/**
 * DAO interface for DLIS.LICENSE_FEE_SCHEDULE table.
 */
public interface LicenseFeeScheduleDao {

    /**
     * Find the currently active fee schedule for a given license type and fee type.
     * Applies effective/expiry date filtering as per AB-PROCESS-PAYMENT.
     */
    Optional<LicenseFeeSchedule> findActiveFee(String licenseType, String feeType);
}
