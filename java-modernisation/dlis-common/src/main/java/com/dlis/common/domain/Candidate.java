package com.dlis.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Candidate domain object — shared across all microservices.
 * Migrated from the monolith's {@code com.dlis.core.domain.Candidate}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate {

    private Long      candidateId;
    private String    firstName;
    private String    lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String    idNumber;
    private String    addressLine1;
    private String    addressLine2;
    private String    city;
    private String    stateProvince;
    private String    postalCode;
    private String    country;
    private String    phoneNumber;
    private String    emailAddress;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate createdDate;

    private String    recordStatus;  // A=Active, I=Inactive

    // ── Constructors ──────────────────────────────────────────────────────────

    public Candidate() {}

    // ── Convenience ───────────────────────────────────────────────────────────

    public boolean isActive() {
        return "A".equals(recordStatus);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long      getCandidateId()   { return candidateId; }
    public void      setCandidateId(Long v)   { this.candidateId = v; }

    public String    getFirstName()     { return firstName; }
    public void      setFirstName(String v)   { this.firstName = v; }

    public String    getLastName()      { return lastName; }
    public void      setLastName(String v)    { this.lastName = v; }

    public LocalDate getDateOfBirth()   { return dateOfBirth; }
    public void      setDateOfBirth(LocalDate v) { this.dateOfBirth = v; }

    public String    getIdNumber()      { return idNumber; }
    public void      setIdNumber(String v)    { this.idNumber = v; }

    public String    getAddressLine1()  { return addressLine1; }
    public void      setAddressLine1(String v){ this.addressLine1 = v; }

    public String    getAddressLine2()  { return addressLine2; }
    public void      setAddressLine2(String v){ this.addressLine2 = v; }

    public String    getCity()          { return city; }
    public void      setCity(String v)        { this.city = v; }

    public String    getStateProvince() { return stateProvince; }
    public void      setStateProvince(String v){ this.stateProvince = v; }

    public String    getPostalCode()    { return postalCode; }
    public void      setPostalCode(String v)  { this.postalCode = v; }

    public String    getCountry()       { return country; }
    public void      setCountry(String v)     { this.country = v; }

    public String    getPhoneNumber()   { return phoneNumber; }
    public void      setPhoneNumber(String v) { this.phoneNumber = v; }

    public String    getEmailAddress()  { return emailAddress; }
    public void      setEmailAddress(String v){ this.emailAddress = v; }

    public LocalDate getCreatedDate()   { return createdDate; }
    public void      setCreatedDate(LocalDate v){ this.createdDate = v; }

    public String    getRecordStatus()  { return recordStatus; }
    public void      setRecordStatus(String v){ this.recordStatus = v; }
}
