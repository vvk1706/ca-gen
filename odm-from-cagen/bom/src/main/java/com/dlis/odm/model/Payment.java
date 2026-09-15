package com.dlis.odm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BOM Class: Payment
 * Converted from CA Gen entity: PAYMENT.ENT
 *
 * Records payment transactions for license applications.
 * Created by the payment processing step; read during final gate checks.
 */
public class Payment {

    /** System-generated primary key (PAYMENT-ID) */
    private long paymentId;

    /** Foreign key to LICENSE-APPLICATION (APPLICATION-ID) */
    private long applicationId;

    /** Foreign key to CANDIDATE (CANDIDATE-ID) */
    private long candidateId;

    /** Date payment was made (PAYMENT-DATE) */
    private LocalDate paymentDate;

    /** Amount paid — must be > 0 (PAYMENT-AMOUNT) */
    private BigDecimal paymentAmount;

    /**
     * Payment method (PAYMENT-METHOD).
     * Valid values: "CC"=Credit Card, "DC"=Debit Card, "EF"=EFT, "CS"=Cash, "CH"=Cheque
     */
    private String paymentMethod;

    /** Bank/gateway reference number (PAYMENT-REFERENCE) */
    private String paymentReference;

    /**
     * Status of payment (PAYMENT-STATUS).
     * Valid values: "S"=Success, "F"=Failed, "R"=Refunded, "P"=Pending
     */
    private String paymentStatus;

    /**
     * License type paid for (LICENSE-TYPE).
     * Valid values: "L", "P", "O"
     */
    private String licenseType;

    /**
     * Type of fee (FEE-TYPE).
     * Valid values: "IF"=Issue, "RF"=Renewal, "LF"=Late, "PF"=Processing
     */
    private String feeType;

    /** System-generated receipt number (RECEIPT-NUMBER) */
    private String receiptNumber;

    /** Officer who processed the payment (PROCESSED-BY) */
    private String processedBy;

    /** Additional notes (NOTES) */
    private String notes;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getPaymentId() { return paymentId; }
    public void setPaymentId(long paymentId) { this.paymentId = paymentId; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
