package com.dlis.core.service;

import com.dlis.core.domain.Candidate;

/**
 * Service interface for candidate management.
 * Implementations are EJB stateless session beans injected into CICS programs
 * and REST resources.
 */
public interface CandidateService {

    /**
     * Create a new candidate record.
     * Business rules derived from AB-CREATE-CANDIDATE:
     *   - ID number must not already be on file for an active candidate
     *   - Date of birth must be in the past
     *
     * @param candidate populated domain object (candidateId must be null)
     * @param createdBy user creating the record
     * @return ServiceResult carrying the generated candidateId on success
     */
    ServiceResult<Long> createCandidate(Candidate candidate, String createdBy);

    /**
     * Find an active candidate by their national ID number.
     */
    ServiceResult<Candidate> findByIdNumber(String idNumber);

    /**
     * Find a candidate by primary key.
     */
    ServiceResult<Candidate> findById(Long candidateId);
}
