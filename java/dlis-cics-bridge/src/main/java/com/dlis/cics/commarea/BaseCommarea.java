package com.dlis.cics.commarea;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Base class for DLIS CICS COMMAREA structures.
 * Provides fixed-width EBCDIC-compatible byte encoding helpers
 * used when interfacing with legacy COBOL programs or native CICS
 * via EXEC CICS LINK / EXEC CICS PUT CONTAINER.
 *
 * All subclass fields must be serialized/deserialized in the exact
 * order defined in the corresponding COBOL copybook (DLISWS.cpy).
 */
public abstract class BaseCommarea implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Max COMMAREA size supported (bytes) — must be ≤ CICS MAXDATASIZE */
    protected static final int MAX_SIZE = 32767;

    // ─── Common header present on every DLIS COMMAREA ─────────────────────
    protected String tranCode;       // CHAR(4)  Transaction id
    protected int    returnCode;     // PIC 9(4)  0=OK, non-zero=error
    protected String returnMessage;  // CHAR(100) Human-readable message
    protected String userId;         // CHAR(20)  Calling user

    // ─── Encoding helpers ─────────────────────────────────────────────────

    protected static byte[] encodeString(String value, int length) {
        byte[] buf = new byte[length];
        Arrays.fill(buf, (byte) 0x40); // EBCDIC space
        if (value != null) {
            byte[] src = value.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(src, 0, buf, 0, Math.min(src.length, length));
        }
        return buf;
    }

    protected static String decodeString(byte[] buf, int offset, int length) {
        return new String(buf, offset, length, StandardCharsets.ISO_8859_1).trim();
    }

    protected static byte[] encodeNumeric(long value, int digits) {
        String formatted = String.format("%0" + digits + "d", value);
        return formatted.getBytes(StandardCharsets.ISO_8859_1);
    }

    protected static long decodeNumeric(byte[] buf, int offset, int length) {
        String s = new String(buf, offset, length, StandardCharsets.ISO_8859_1).trim();
        return s.isEmpty() ? 0L : Long.parseLong(s);
    }

    // ─── Abstract: produce the raw byte COMMAREA ──────────────────────────
    public abstract byte[] toBytes();

    public abstract void fromBytes(byte[] data);

    // ─── Getters / Setters for header fields ──────────────────────────────

    public String getTranCode()             { return tranCode; }
    public void   setTranCode(String v)     { this.tranCode = v; }

    public int  getReturnCode()             { return returnCode; }
    public void setReturnCode(int v)        { this.returnCode = v; }

    public String getReturnMessage()        { return returnMessage; }
    public void   setReturnMessage(String v){ this.returnMessage = v; }

    public String getUserId()               { return userId; }
    public void   setUserId(String v)       { this.userId = v; }
}
