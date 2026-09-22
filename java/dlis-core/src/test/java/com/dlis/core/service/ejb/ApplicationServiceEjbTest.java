package com.dlis.core.service.ejb;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.dao.CandidateDao;
import com.dlis.core.dao.DrivingHistoryDao;
import com.dlis.core.dao.IssuedLicenseDao;
import com.dlis.core.domain.*;
import com.dlis.core.service.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApplicationServiceEjb — eligibility and history check logic.
 * Business rules verified against AB-CHECK-ELIGIBILITY and AB-CHECK-HISTORY.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceEjbTest {

    @Mock private ApplicationDao    applicationDao;
    @Mock private CandidateDao      candidateDao;
    @Mock private DrivingHistoryDao historyDao;
    @Mock private IssuedLicenseDao  licenseDao;

    @InjectMocks
    private ApplicationServiceEjb service;

    private Candidate candidate16;
    private LicenseApplication learnerApp;

    @BeforeEach
    void setUp() {
        candidate16 = new Candidate();
        candidate16.setCandidateId(1L);
        candidate16.setDateOfBirth(LocalDate.now().minusYears(16));
        candidate16.setRecordStatus("A");

        learnerApp = new LicenseApplication();
        learnerApp.setApplicationId(100L);
        learnerApp.setCandidateId(1L);
        learnerApp.setLicenseType("L");
        learnerApp.setApplicationStatus(LicenseApplication.STATUS_PENDING);
        learnerApp.setEligibilityChkStatus("U");
        learnerApp.setHistoryChkStatus("U");
        learnerApp.setPaymentStatus("U");
    }

    // ── createApplication ────────────────────────────────────────────────────

    @Test
    void createApplication_success() {
        when(candidateDao.findById(1L)).thenReturn(Optional.of(candidate16));
        when(applicationDao.findActiveByCandidate(1L, "L"))
                .thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            LicenseApplication a = inv.getArgument(0);
            a.setApplicationId(100L);
            return null;
        }).when(applicationDao).insert(any());

        ServiceResult<Long> result = service.createApplication(1L, "L", "OFFICER1");

        assertTrue(result.isOk());
        assertEquals(100L, result.getPayload());
    }

    @Test
    void createApplication_candidateNotFound() {
        when(candidateDao.findById(99L)).thenReturn(Optional.empty());

        ServiceResult<Long> result = service.createApplication(99L, "L", "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    @Test
    void createApplication_invalidLicenseType() {
        when(candidateDao.findById(1L)).thenReturn(Optional.of(candidate16));

        ServiceResult<Long> result = service.createApplication(1L, "X", "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_TYPE, result.getReturnCode());
    }

    // ── checkEligibility ─────────────────────────────────────────────────────

    @Test
    void checkEligibility_learner_16yo_passes() {
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));
        when(candidateDao.findById(1L)).thenReturn(Optional.of(candidate16));

        ServiceResult<String> result = service.checkEligibility(100L, "OFFICER1");

        assertTrue(result.isOk());
        assertEquals("P", result.getPayload());
        verify(applicationDao).updateEligibilityCheck(
                eq(100L), eq("P"), any(), anyString(),
                eq(LicenseApplication.STATUS_ELIG_CHECKED), any(), anyString());
    }

    @Test
    void checkEligibility_learner_underage_fails() {
        candidate16.setDateOfBirth(LocalDate.now().minusYears(15)); // only 15
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));
        when(candidateDao.findById(1L)).thenReturn(Optional.of(candidate16));

        ServiceResult<String> result = service.checkEligibility(100L, "OFFICER1");

        assertTrue(result.isOk()); // service returns ok with F status
        assertEquals("F", result.getPayload());
        verify(applicationDao).updateEligibilityCheck(
                eq(100L), eq("F"), any(), anyString(),
                eq(LicenseApplication.STATUS_REJECTED), any(), anyString());
    }

    @Test
    void checkEligibility_probation_requiresActiveLearner() {
        LicenseApplication probApp = new LicenseApplication();
        probApp.setApplicationId(101L);
        probApp.setCandidateId(1L);
        probApp.setLicenseType("P");
        probApp.setEligibilityChkStatus("U");

        Candidate c17 = new Candidate();
        c17.setCandidateId(1L);
        c17.setDateOfBirth(LocalDate.now().minusYears(17));
        c17.setRecordStatus("A");

        when(applicationDao.findById(101L)).thenReturn(Optional.of(probApp));
        when(candidateDao.findById(1L)).thenReturn(Optional.of(c17));
        // No active Learner license
        when(licenseDao.findActiveByCandidate(1L, "L"))
                .thenReturn(Collections.emptyList());

        ServiceResult<String> result = service.checkEligibility(101L, "OFFICER1");

        assertEquals("F", result.getPayload());
        assertTrue(result.getMessage().contains("LEARNER"));
    }

    // ── checkHistory ──────────────────────────────────────────────────────────

    @Test
    void checkHistory_noRecords_passes() {
        learnerApp.setEligibilityChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));
        when(historyDao.findActiveByCandidateId(1L))
                .thenReturn(Collections.emptyList());

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertTrue(result.isOk());
        assertEquals("P", result.getPayload());
    }

    @Test
    void checkHistory_unpaidFine_fails() {
        learnerApp.setEligibilityChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        DrivingHistory badHistory = new DrivingHistory();
        badHistory.setIncidentType("OF");
        badHistory.setDemeritPoints(2);
        badHistory.setFineAmount(new BigDecimal("500.00"));
        badHistory.setFinePaidStatus("N");
        badHistory.setRecordStatus("A");

        when(historyDao.findActiveByCandidateId(1L))
                .thenReturn(List.of(badHistory));

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertEquals("F", result.getPayload());
        assertTrue(result.getMessage().contains("UNPAID FINES"));
    }

    @Test
    void checkHistory_demeritExceeds12_fails() {
        learnerApp.setEligibilityChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        DrivingHistory h = new DrivingHistory();
        h.setIncidentType("OF");
        h.setDemeritPoints(13);
        h.setFineAmount(BigDecimal.ZERO);
        h.setRecordStatus("A");

        when(historyDao.findActiveByCandidateId(1L)).thenReturn(List.of(h));

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertEquals("F", result.getPayload());
        assertTrue(result.getMessage().contains("DEMERIT"));
    }

    @Test
    void checkHistory_eligibilityNotPassed_returnsError() {
        learnerApp.setEligibilityChkStatus("U");  // not passed
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertFalse(result.isOk());
        assertTrue(result.getMessage().contains("ELIGIBILITY"));
    }
}
