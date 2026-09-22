package com.dlis.core.service.ejb;

import com.dlis.core.dao.CandidateDao;
import com.dlis.core.domain.Candidate;
import com.dlis.core.service.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CandidateServiceEjb.
 * Derived from the business rules in AB-CREATE-CANDIDATE.
 */
@ExtendWith(MockitoExtension.class)
class CandidateServiceEjbTest {

    @Mock
    private CandidateDao candidateDao;

    @InjectMocks
    private CandidateServiceEjb service;

    private Candidate validCandidate;

    @BeforeEach
    void setUp() {
        validCandidate = new Candidate();
        validCandidate.setFirstName("Jane");
        validCandidate.setLastName("Smith");
        validCandidate.setDateOfBirth(LocalDate.of(1990, 5, 15));
        validCandidate.setIdNumber("ID12345678");
        validCandidate.setAddressLine1("123 Main Street");
        validCandidate.setCity("Johannesburg");
        validCandidate.setStateProvince("Gauteng");
        validCandidate.setPostalCode("2000");
        validCandidate.setCountry("ZA");
    }

    // ── createCandidate ───────────────────────────────────────────────────────

    @Test
    void createCandidate_success() {
        when(candidateDao.findByIdNumber("ID12345678")).thenReturn(Optional.empty());
        doAnswer(inv -> {
            Candidate c = inv.getArgument(0);
            c.setCandidateId(1001L);
            return null;
        }).when(candidateDao).insert(any(Candidate.class));

        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        assertTrue(result.isOk());
        assertEquals(1001L, result.getPayload());
        assertEquals("CANDIDATE CREATED SUCCESSFULLY", result.getMessage());
        verify(candidateDao).insert(validCandidate);
    }

    @Test
    void createCandidate_duplicateIdNumber_returnsError() {
        when(candidateDao.findByIdNumber("ID12345678"))
                .thenReturn(Optional.of(validCandidate));

        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_DUPLICATE, result.getReturnCode());
        assertTrue(result.getMessage().contains("ALREADY EXISTS"));
        verify(candidateDao, never()).insert(any());
    }

    @Test
    void createCandidate_futureDateOfBirth_returnsError() {
        when(candidateDao.findByIdNumber(any())).thenReturn(Optional.empty());
        validCandidate.setDateOfBirth(LocalDate.now().plusDays(1));

        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_AGE, result.getReturnCode());
        verify(candidateDao, never()).insert(any());
    }

    @Test
    void createCandidate_nullDateOfBirth_returnsError() {
        when(candidateDao.findByIdNumber(any())).thenReturn(Optional.empty());
        validCandidate.setDateOfBirth(null);

        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_AGE, result.getReturnCode());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_found() {
        validCandidate.setCandidateId(42L);
        when(candidateDao.findById(42L)).thenReturn(Optional.of(validCandidate));

        ServiceResult<Candidate> result = service.findById(42L);

        assertTrue(result.isOk());
        assertEquals("Jane", result.getPayload().getFirstName());
    }

    @Test
    void findById_notFound() {
        when(candidateDao.findById(99L)).thenReturn(Optional.empty());

        ServiceResult<Candidate> result = service.findById(99L);

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }
}
