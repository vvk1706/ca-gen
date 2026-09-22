package com.dlis.core.dao;

import com.dlis.core.domain.IssuedLicense;
import java.util.List;
import java.util.Optional;

/**
 * DAO interface for DLIS.ISSUED_LICENSE table.
 */
public interface IssuedLicenseDao {

    void insert(IssuedLicense license);

    Optional<IssuedLicense> findById(Long licenseId);

    Optional<IssuedLicense> findByLicenseNumber(String licenseNumber);

    /** Find active licenses by candidate and type — used for upgrade path validation. */
    List<IssuedLicense> findActiveByCandidate(Long candidateId, String licenseType);
}
