package com.dlis.cics.program;

import com.ibm.cics.server.*;
import com.dlis.cics.commarea.CandidateCommarea;
import com.dlis.core.domain.Candidate;
import com.dlis.core.service.CandidateService;
import com.dlis.core.service.ServiceResult;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * CICS Java Program: DLISCAND
 * Transaction: CAND
 *
 * Entry point for Candidate Maintenance.
 * Receives a CandidateCommarea, dispatches to CandidateService,
 * fills the response in the same COMMAREA and returns.
 *
 * This class is a JCICS MainProgram — it implements Executable and is
 * called by CICS via EXEC CICS LINK PROGRAM('DLISCAND').
 */
public class DlisCandProgram implements Executable {

    @Inject
    private CandidateService candidateService;

    @Override
    public void execute(CommAreaHolder commArea) throws InvalidRequestException {

        CandidateCommarea ca = new CandidateCommarea();

        // Decode the inbound COMMAREA bytes
        if (commArea != null && commArea.getValue() != null) {
            ca.fromBytes(commArea.getValue());
        }

        try {
            dispatchByTranCode(ca);
        } catch (Exception e) {
            ca.setReturnCode(99);
            ca.setReturnMessage(truncate("INTERNAL ERROR: " + e.getMessage(), 100));
        }

        // Write response back to COMMAREA
        if (commArea != null) {
            commArea.setValue(ca.toBytes());
        }
    }

    private void dispatchByTranCode(CandidateCommarea ca) {
        switch (ca.getTranCode() != null ? ca.getTranCode().trim() : "") {
            case "CAND":
            case "CADD":
                handleCreate(ca);
                break;
            case "CINQ":
                handleInquire(ca);
                break;
            default:
                ca.setReturnCode(99);
                ca.setReturnMessage("UNKNOWN TRANSACTION CODE: " + ca.getTranCode());
        }
    }

    // ── Create Candidate ──────────────────────────────────────────────────────
    private void handleCreate(CandidateCommarea ca) {
        Candidate c = new Candidate();
        c.setFirstName(ca.getFirstName());
        c.setLastName(ca.getLastName());

        // Parse CCYYMMDD date
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
            c.setDateOfBirth(LocalDate.parse(ca.getDateOfBirth().trim(), fmt));
        } catch (Exception e) {
            ca.setReturnCode(2);
            ca.setReturnMessage("INVALID DATE OF BIRTH FORMAT - EXPECTED YYYYMMDD");
            return;
        }

        c.setIdNumber(ca.getIdNumber());
        c.setAddressLine1(ca.getAddressLine1());
        c.setAddressLine2(ca.getAddressLine2());
        c.setCity(ca.getCity());
        c.setPhoneNumber(ca.getPhoneNumber());
        c.setEmailAddress(ca.getEmailAddress());

        ServiceResult<Long> result = candidateService.createCandidate(c, ca.getUserId());

        ca.setReturnCode(result.getReturnCode());
        ca.setReturnMessage(truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            ca.setCandidateId(result.getPayload());
        }
    }

    // ── Inquire Candidate ─────────────────────────────────────────────────────
    private void handleInquire(CandidateCommarea ca) {
        ServiceResult<Candidate> result = candidateService.findById(ca.getCandidateId());

        ca.setReturnCode(result.getReturnCode());
        ca.setReturnMessage(truncate(result.getMessage(), 100));

        if (result.isOk() && result.getPayload() != null) {
            Candidate c = result.getPayload();
            ca.setFirstName(c.getFirstName());
            ca.setLastName(c.getLastName());
            if (c.getDateOfBirth() != null) {
                ca.setDateOfBirth(c.getDateOfBirth()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            }
            ca.setIdNumber(c.getIdNumber());
            ca.setAddressLine1(c.getAddressLine1());
            ca.setAddressLine2(c.getAddressLine2());
            ca.setCity(c.getCity());
            ca.setPhoneNumber(c.getPhoneNumber());
            ca.setEmailAddress(c.getEmailAddress());
            ca.setRecordStatus(c.getRecordStatus());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
