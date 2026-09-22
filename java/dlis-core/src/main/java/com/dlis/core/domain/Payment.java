package com.dlis.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model for PAYMENT entity.
 * Maps to DLIS.PAYMENT in DB2.
 */
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long        paymentId;
    private Long        applicationId;
    private Long        candidateId;
    private LocalDate   paymentDate;
    private BigDecimal  paymentAmount;
    private String      paymentMethod;     // CC, DC, EF, CS, CH
    private String      paymentReference;
    private String      paymentStatus;     // S, F, R, P
    private String      licenseType;       // L, P, O
    private String      feeType;           // IF, RF, LF, PF
    private String      receiptNumber;
    private String      processedBy;
    private String      notes;

    public Payment() {}

    // ---- Getters / Setters ----

    public Long getPaymentId()                          { return paymentId; }
    public void setPaymentId(Long v)                    { this.paymentId = v; }

    public Long getApplicationId()                      { return applicationId; }
    public void setApplicationId(Long v)                { this.applicationId = v; }

    public Long getCandidateId()                        { return candidateId; }
    public void setCandidateId(Long v)                  { this.candidateId = v; }

    public LocalDate getPaymentDate()                   { return paymentDate; }
    public void setPaymentDate(LocalDate v)             { this.paymentDate = v; }

    public BigDecimal getPaymentAmount()                { return paymentAmount; }
    public void setPaymentAmount(BigDecimal v)          { this.paymentAmount = v; }

    public String getPaymentMethod()                    { return paymentMethod; }
    public void setPaymentMethod(String v)              { this.paymentMethod = v; }

    public String getPaymentReference()                 { return paymentReference; }
    public void setPaymentReference(String v)           { this.paymentReference = v; }

    public String getPaymentStatus()                    { return paymentStatus; }
    public void setPaymentStatus(String v)              { this.paymentStatus = v; }

    public String getLicenseType()                      { return licenseType; }
    public void setLicenseType(String v)                { this.licenseType = v; }

    public String getFeeType()                          { return feeType; }
    public void setFeeType(String v)                    { this.feeType = v; }

    public String getReceiptNumber()                    { return receiptNumber; }
    public void setReceiptNumber(String v)              { this.receiptNumber = v; }

    public String getProcessedBy()                      { return processedBy; }
    public void setProcessedBy(String v)                { this.processedBy = v; }

    public String getNotes()                            { return notes; }
    public void setNotes(String v)                      { this.notes = v; }
}
