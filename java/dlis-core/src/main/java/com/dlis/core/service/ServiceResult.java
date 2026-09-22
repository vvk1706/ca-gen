package com.dlis.core.service;

import java.io.Serializable;

/**
 * Generic service result wrapper used by all DLIS service methods.
 *
 * @param <T> type of the payload returned on success
 */
public class ServiceResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // Well-known return codes matching the CA Gen action block conventions
    public static final int RC_OK                  = 0;
    public static final int RC_NOT_FOUND           = 1;
    public static final int RC_DUPLICATE           = 1;
    public static final int RC_INVALID_AGE         = 2;
    public static final int RC_INVALID_TYPE        = 2;
    public static final int RC_FEE_NOT_FOUND       = 2;
    public static final int RC_ALREADY_EXISTS      = 3;
    public static final int RC_ALREADY_PAID        = 3;
    public static final int RC_HISTORY_NOT_PASSED  = 4;
    public static final int RC_PAYMENT_NOT_DONE    = 4;
    public static final int RC_NOT_AUTHORISED      = 3;
    public static final int RC_INVALID_DECISION    = 5;
    public static final int RC_ERROR               = 99;

    private final int     returnCode;
    private final String  message;
    private final T       payload;

    private ServiceResult(int returnCode, String message, T payload) {
        this.returnCode = returnCode;
        this.message    = message;
        this.payload    = payload;
    }

    public static <T> ServiceResult<T> ok(T payload, String message) {
        return new ServiceResult<>(RC_OK, message, payload);
    }

    public static <T> ServiceResult<T> ok(T payload) {
        return new ServiceResult<>(RC_OK, "OK", payload);
    }

    public static <T> ServiceResult<T> error(int returnCode, String message) {
        return new ServiceResult<>(returnCode, message, null);
    }

    public boolean isOk()    { return returnCode == RC_OK; }
    public boolean isError() { return returnCode != RC_OK; }

    public int    getReturnCode() { return returnCode; }
    public String getMessage()    { return message; }
    public T      getPayload()    { return payload; }

    @Override
    public String toString() {
        return "ServiceResult{rc=" + returnCode + ", msg='" + message + "', payload=" + payload + "}";
    }
}
