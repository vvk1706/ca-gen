package com.dlis.odm.model;

import java.time.LocalDate;

/**
 * BOM Class: Candidate
 * Converted from CA Gen entity: CANDIDATE.ENT
 *
 * Represents a person registering to apply for a driver license.
 * Rule visibility: read-only from rule perspective (created via REST/CICS layer).
 */
public class Candidate {

    /** System-generated primary key (CANDIDATE-ID) */
    private long candidateId;

    /** Legal first name (FIRST-NAME) */
    private String firstName;

    /** Legal last name (LAST-NAME) */
    private String lastName;

    /** Date of birth — must be in the past (DATE-OF-BIRTH) */
    private LocalDate dateOfBirth;

    /** National ID or Passport number — must be unique (ID-NUMBER) */
    private String idNumber;

    /** Street address (ADDRESS-LINE-1) */
    private String addressLine1;

    /** Additional address line (ADDRESS-LINE-2) */
    private String addressLine2;

    /** City (CITY) */
    private String city;

    /** State or Province (STATE-PROVINCE) */
    private String stateProvince;

    /** Postal / ZIP code (POSTAL-CODE) */
    private String postalCode;

    /** Country (COUNTRY) */
    private String country;

    /** Contact phone (PHONE-NUMBER) */
    private String phoneNumber;

    /** Contact email (EMAIL-ADDRESS) */
    private String emailAddress;

    /** Date record was created (CREATED-DATE) */
    private LocalDate createdDate;

    /**
     * Active/Inactive flag (RECORD-STATUS).
     * Valid values: "A"=Active, "I"=Inactive
     */
    private String recordStatus;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateProvince() { return stateProvince; }
    public void setStateProvince(String stateProvince) { this.stateProvince = stateProvince; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public String getRecordStatus() { return recordStatus; }
    public void setRecordStatus(String recordStatus) { this.recordStatus = recordStatus; }
}
