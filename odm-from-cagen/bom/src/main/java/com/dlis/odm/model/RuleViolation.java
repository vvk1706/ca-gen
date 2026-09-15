package com.dlis.odm.model;

/**
 * BOM Class: RuleViolation
 *
 * Represents a single rule violation recorded during ruleset execution.
 * Accumulated in RuleContext.violations during processing.
 * Maps directly to the CA Gen error/return message pattern.
 */
public class RuleViolation {

    /** Business rule ID (e.g., "BR-02") */
    private String ruleId;

    /** Human-readable violation message (mirrors CA Gen return messages) */
    private String message;

    /**
     * Severity level.
     * Valid values: "ERROR"=hard stop, "WARNING"=informational only
     */
    private String severity;

    /** The field or attribute that caused the violation (optional) */
    private String fieldName;

    public RuleViolation() {}

    public RuleViolation(String ruleId, String message, String severity) {
        this.ruleId = ruleId;
        this.message = message;
        this.severity = severity;
    }

    public RuleViolation(String ruleId, String message, String severity, String fieldName) {
        this.ruleId = ruleId;
        this.message = message;
        this.severity = severity;
        this.fieldName = fieldName;
    }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    @Override
    public String toString() {
        return "[" + ruleId + "] " + severity + ": " + message;
    }
}
