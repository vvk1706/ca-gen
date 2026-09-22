package com.dlis.common.api;

/**
 * Generic service result wrapper — replaces EJB-era {@code ServiceResult<T>}.
 * Used consistently across all microservices as the internal service return type;
 * REST resources translate it to the appropriate HTTP response code + JSON body.
 */
public class ServiceResult<T> {

    public static final int RC_OK               = 0;
    public static final int RC_NOT_FOUND        = 4;
    public static final int RC_DUPLICATE        = 8;
    public static final int RC_ALREADY_EXISTS   = 8;
    public static final int RC_ERROR            = 12;
    public static final int RC_INVALID_TYPE     = 1;
    public static final int RC_INVALID_AGE      = 2;
    public static final int RC_INVALID_DECISION = 3;
    public static final int RC_PAYMENT_NOT_DONE = 5;
    public static final int RC_HISTORY_NOT_PASSED = 6;
    public static final int RC_ALREADY_PAID     = 7;
    public static final int RC_FEE_NOT_FOUND    = 9;
    public static final int RC_NOT_AUTHORISED   = 10;

    private final int    returnCode;
    private final String message;
    private final T      payload;

    private ServiceResult(int rc, String msg, T payload) {
        this.returnCode = rc;
        this.message    = msg;
        this.payload    = payload;
    }

    public static <T> ServiceResult<T> ok(T payload, String msg) {
        return new ServiceResult<>(RC_OK, msg, payload);
    }

    public static <T> ServiceResult<T> error(int rc, String msg) {
        return new ServiceResult<>(rc, msg, null);
    }

    public boolean isOk()          { return returnCode == RC_OK; }
    public int     getReturnCode() { return returnCode; }
    public String  getMessage()    { return message; }
    public T       getPayload()    { return payload; }
}
