package com.dlis.odm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BOM Class: DrivingHistory
 * Converted from CA Gen entity: DRIVING-HISTORY.ENT
 *
 * Records prior road incidents, fines, suspensions, and demerit points
 * for a candidate. Evaluated during the History Check ruleflow.
 */
public class DrivingHistory {

    /** System-generated primary key (HISTORY-ID) */
    private long historyId;

    /** Foreign key to CANDIDATE (CANDIDATE-ID) */
    private long candidateId;

    /** Date of incident (INCIDENT-DATE) */
    private LocalDate incidentDate;

    /**
     * Type of incident (INCIDENT-TYPE).
     * Valid values: "OF"=Offence, "AC"=Accident, "SU"=Suspension, "DQ"=Disqualification
     */
    private String incidentType;

    /** Description of incident (INCIDENT-DESCRIPTION) */
    private String incidentDescription;

    /** Demerit points assigned — must be >= 0 (DEMERIT-POINTS) */
    private int demeritPoints;

    /** Fine amount issued — must be >= 0 (FINE-AMOUNT) */
    private BigDecimal fineAmount;

    /**
     * Fine payment status (FINE-PAID-STATUS).
     * Valid values: "Y"=Paid, "N"=Unpaid
     */
    private String finePaidStatus;

    /** Start of suspension period (SUSPENSION-START-DATE) */
    private LocalDate suspensionStartDate;

    /**
     * End of suspension period — null means indefinite (SUSPENSION-END-DATE).
     * If null and incident type is SU/DQ, the suspension is considered active.
     */
    private LocalDate suspensionEndDate;

    /** Court reference number if applicable (COURT-CASE-NUMBER) */
    private String courtCaseNumber;

    /** Authority who recorded this incident (RECORDED-BY-AUTHORITY) */
    private String recordedByAuthority;

    /**
     * Active/Inactive flag (RECORD-STATUS).
     * Valid values: "A"=Active, "I"=Inactive
     */
    private String recordStatus;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getHistoryId() { return historyId; }
    public void setHistoryId(long historyId) { this.historyId = historyId; }

    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }

    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public String getIncidentDescription() { return incidentDescription; }
    public void setIncidentDescription(String incidentDescription) { this.incidentDescription = incidentDescription; }

    public int getDemeritPoints() { return demeritPoints; }
    public void setDemeritPoints(int demeritPoints) { this.demeritPoints = demeritPoints; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public String getFinePaidStatus() { return finePaidStatus; }
    public void setFinePaidStatus(String finePaidStatus) { this.finePaidStatus = finePaidStatus; }

    public LocalDate getSuspensionStartDate() { return suspensionStartDate; }
    public void setSuspensionStartDate(LocalDate suspensionStartDate) { this.suspensionStartDate = suspensionStartDate; }

    public LocalDate getSuspensionEndDate() { return suspensionEndDate; }
    public void setSuspensionEndDate(LocalDate suspensionEndDate) { this.suspensionEndDate = suspensionEndDate; }

    public String getCourtCaseNumber() { return courtCaseNumber; }
    public void setCourtCaseNumber(String courtCaseNumber) { this.courtCaseNumber = courtCaseNumber; }

    public String getRecordedByAuthority() { return recordedByAuthority; }
    public void setRecordedByAuthority(String recordedByAuthority) { this.recordedByAuthority = recordedByAuthority; }

    public String getRecordStatus() { return recordStatus; }
    public void setRecordStatus(String recordStatus) { this.recordStatus = recordStatus; }
}
