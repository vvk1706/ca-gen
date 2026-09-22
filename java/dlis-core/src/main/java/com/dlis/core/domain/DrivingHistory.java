package com.dlis.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model for DRIVING_HISTORY entity.
 * Maps to DLIS.DRIVING_HISTORY in DB2.
 */
public class DrivingHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_OFFENCE         = "OF";
    public static final String TYPE_ACCIDENT        = "AC";
    public static final String TYPE_SUSPENSION      = "SU";
    public static final String TYPE_DISQUALIFICATION= "DQ";

    private Long        historyId;
    private Long        candidateId;
    private LocalDate   incidentDate;
    private String      incidentType;
    private String      incidentDescription;
    private Integer     demeritPoints;
    private BigDecimal  fineAmount;
    private String      finePaidStatus;    // Y, N
    private LocalDate   suspensionStartDate;
    private LocalDate   suspensionEndDate;
    private String      courtCaseNumber;
    private String      recordedByAuthority;
    private String      recordStatus;      // A, I

    public DrivingHistory() {}

    public boolean isActiveSuspensionOrDisqualification() {
        if (!TYPE_SUSPENSION.equals(incidentType) && !TYPE_DISQUALIFICATION.equals(incidentType)) {
            return false;
        }
        return suspensionEndDate == null || !suspensionEndDate.isBefore(LocalDate.now());
    }

    public boolean hasUnpaidFine() {
        return fineAmount != null
            && fineAmount.compareTo(BigDecimal.ZERO) > 0
            && "N".equals(finePaidStatus);
    }

    // ---- Getters / Setters ----

    public Long getHistoryId()                          { return historyId; }
    public void setHistoryId(Long v)                    { this.historyId = v; }

    public Long getCandidateId()                        { return candidateId; }
    public void setCandidateId(Long v)                  { this.candidateId = v; }

    public LocalDate getIncidentDate()                  { return incidentDate; }
    public void setIncidentDate(LocalDate v)            { this.incidentDate = v; }

    public String getIncidentType()                     { return incidentType; }
    public void setIncidentType(String v)               { this.incidentType = v; }

    public String getIncidentDescription()              { return incidentDescription; }
    public void setIncidentDescription(String v)        { this.incidentDescription = v; }

    public Integer getDemeritPoints()                   { return demeritPoints; }
    public void setDemeritPoints(Integer v)             { this.demeritPoints = v; }

    public BigDecimal getFineAmount()                   { return fineAmount; }
    public void setFineAmount(BigDecimal v)             { this.fineAmount = v; }

    public String getFinePaidStatus()                   { return finePaidStatus; }
    public void setFinePaidStatus(String v)             { this.finePaidStatus = v; }

    public LocalDate getSuspensionStartDate()           { return suspensionStartDate; }
    public void setSuspensionStartDate(LocalDate v)     { this.suspensionStartDate = v; }

    public LocalDate getSuspensionEndDate()             { return suspensionEndDate; }
    public void setSuspensionEndDate(LocalDate v)       { this.suspensionEndDate = v; }

    public String getCourtCaseNumber()                  { return courtCaseNumber; }
    public void setCourtCaseNumber(String v)            { this.courtCaseNumber = v; }

    public String getRecordedByAuthority()              { return recordedByAuthority; }
    public void setRecordedByAuthority(String v)        { this.recordedByAuthority = v; }

    public String getRecordStatus()                     { return recordStatus; }
    public void setRecordStatus(String v)               { this.recordStatus = v; }
}
