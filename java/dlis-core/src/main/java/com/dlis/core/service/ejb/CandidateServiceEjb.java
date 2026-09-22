package com.dlis.core.service.ejb;

import com.dlis.core.dao.CandidateDao;
import com.dlis.core.domain.Candidate;
import com.dlis.core.service.CandidateService;
import com.dlis.core.service.ServiceResult;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

/**
 * EJB implementation of CandidateService.
 * Directly maps logic from CA Gen AB-CREATE-CANDIDATE.
 * All DB operations run inside the caller's CICS/WAS transaction
 * (TransactionAttributeType.REQUIRED inherits the outer JTA/XA transaction).
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CandidateServiceEjb implements CandidateService {

    @Inject
    private CandidateDao candidateDao;

    @Override
    public ServiceResult<Long> createCandidate(Candidate candidate, String createdBy) {

        // 1. Duplicate ID number check (AB-CREATE-CANDIDATE rule)
        Optional<Candidate> dup = candidateDao.findByIdNumber(candidate.getIdNumber());
        if (dup.isPresent()) {
            return ServiceResult.error(ServiceResult.RC_DUPLICATE,
                    "CANDIDATE WITH THIS ID NUMBER ALREADY EXISTS");
        }

        // 2. Date of birth must be in the past (AB-CREATE-CANDIDATE rule)
        if (candidate.getDateOfBirth() == null
                || !candidate.getDateOfBirth().isBefore(LocalDate.now())) {
            return ServiceResult.error(ServiceResult.RC_INVALID_AGE,
                    "DATE OF BIRTH MUST BE IN THE PAST");
        }

        // 3. Set defaults
        candidate.setCreatedDate(LocalDate.now());
        candidate.setRecordStatus("A");

        // 4. Persist
        candidateDao.insert(candidate);

        return ServiceResult.ok(candidate.getCandidateId(),
                "CANDIDATE CREATED SUCCESSFULLY");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ServiceResult<Candidate> findByIdNumber(String idNumber) {
        return candidateDao.findByIdNumber(idNumber)
                .map(c -> ServiceResult.ok(c, "OK"))
                .orElse(ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                        "CANDIDATE NOT FOUND"));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ServiceResult<Candidate> findById(Long candidateId) {
        return candidateDao.findById(candidateId)
                .map(c -> ServiceResult.ok(c, "OK"))
                .orElse(ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                        "CANDIDATE NOT FOUND"));
    }
}
