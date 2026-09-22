package com.dlis.core.dao;

import com.dlis.core.domain.Candidate;
import java.util.Optional;

/**
 * DAO interface for DLIS.CANDIDATE table.
 * JDBC implementation uses DB2 LUW 11.5 Type 4 driver on zLinux.
 */
public interface CandidateDao {

    /** Insert new candidate; generated identity value is set on the passed object. */
    void insert(Candidate candidate);

    Optional<Candidate> findById(Long candidateId);

    Optional<Candidate> findByIdNumber(String idNumber);

    /** Partial update: record_status only. */
    void updateStatus(Long candidateId, String status);
}
