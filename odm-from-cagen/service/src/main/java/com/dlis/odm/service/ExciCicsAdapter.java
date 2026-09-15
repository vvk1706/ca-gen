package com.dlis.odm.service;

import com.ibm.cics.server.CCSIDErrorException;
import com.ibm.cics.server.ChannelErrorException;
import com.ibm.cics.server.CicsConditionException;
import com.ibm.cics.server.Program;
import com.ibm.cics.server.Task;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ExciCicsAdapter
 *
 * Provides EXEC CICS LINK connectivity from the Java ODM REST layer
 * to mainframe CICS programs via the EXCI (External CICS Interface).
 *
 * Used by:
 *   - DlisPaymentService   → links to DLISPAY (payment writes)
 *   - DlisIssuanceService  → links to DLISLICS (license issuance writes)
 *   - DlisLookupService    → links to DLISQLUP (pre-rule data retrieval)
 *
 * Configuration: CICS connection details injected via environment variables:
 *   CICS_APPLID       — VTAM applid of the CICS region (e.g., CICSPROD)
 *   CICS_USERID       — RACF user ID for EXCI invocation
 *   CICS_COMMAREA_MAX — Maximum COMMAREA size in bytes (default: 32767)
 *
 * The EXCI bridge requires:
 *   1. IBM CICS Transaction Gateway (CTG) or equivalent EXCI library
 *   2. TCP/IP connectivity to the mainframe CICS region
 *   3. RACF authorisation for the calling user
 */
public class ExciCicsAdapter {

    private static final int  DEFAULT_COMMAREA_SIZE = 32767;
    private static final String ENCODING              = "IBM-037"; // EBCDIC codepage

    private final String cicsApplid;
    private final String cicsUserId;
    private final int    commareaSize;

    public ExciCicsAdapter() {
        this.cicsApplid   = System.getenv().getOrDefault("CICS_APPLID",       "CICSPROD");
        this.cicsUserId   = System.getenv().getOrDefault("CICS_USERID",       "DLISUSR");
        this.commareaSize = Integer.parseInt(
            System.getenv().getOrDefault("CICS_COMMAREA_MAX", String.valueOf(DEFAULT_COMMAREA_SIZE)));
    }

    /**
     * Invokes a CICS program via EXEC CICS LINK.
     *
     * @param programName  8-character CICS program name (e.g., "DLISPAY ")
     * @param commarea     Byte array COMMAREA to pass (input/output)
     * @return Updated COMMAREA bytes returned by the CICS program
     * @throws CicsLinkException on CICS or transport error
     */
    public byte[] link(String programName, byte[] commarea) throws CicsLinkException {
        try {
            Program program = new Program();
            program.setName(padRight(programName, 8));

            // Copy COMMAREA into a fixed-size buffer
            byte[] commareaBuffer = new byte[commareaSize];
            System.arraycopy(commarea, 0, commareaBuffer, 0,
                Math.min(commarea.length, commareaSize));

            program.link(commareaBuffer);
            return commareaBuffer;

        } catch (CicsConditionException | CCSIDErrorException |
                 ChannelErrorException e) {
            throw new CicsLinkException(
                "EXEC CICS LINK failed for program [" + programName + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes a Java String to EBCDIC bytes for COMMAREA construction.
     */
    public byte[] toEbcdic(String value, int fieldLength) {
        try {
            byte[] ebcdic = value.getBytes(ENCODING);
            byte[] result = new byte[fieldLength];
            System.arraycopy(ebcdic, 0, result, 0, Math.min(ebcdic.length, fieldLength));
            // Pad remainder with EBCDIC space (0x40)
            for (int i = ebcdic.length; i < fieldLength; i++) {
                result[i] = 0x40;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("EBCDIC encoding failed", e);
        }
    }

    /**
     * Decodes EBCDIC bytes from a COMMAREA position to a Java String.
     */
    public String fromEbcdic(byte[] commarea, int offset, int length) {
        try {
            return new String(commarea, offset, length, ENCODING).trim();
        } catch (Exception e) {
            throw new RuntimeException("EBCDIC decoding failed", e);
        }
    }

    /**
     * Reads a packed decimal (COMP-3) value from COMMAREA.
     */
    public long readComp3(byte[] commarea, int offset, int digits) {
        // Simplified COMP-3 reader — (digits+2)/2 bytes
        int byteLen = (digits + 2) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + byteLen - 1; i++) {
            sb.append((commarea[i] >> 4) & 0x0F);
            sb.append(commarea[i] & 0x0F);
        }
        // Last byte: high nibble = last digit, low nibble = sign
        sb.append((commarea[offset + byteLen - 1] >> 4) & 0x0F);
        return Long.parseLong(sb.toString());
    }

    /**
     * Writes a packed decimal (COMP-3) value into COMMAREA.
     */
    public void writeComp3(byte[] commarea, int offset, long value, int digits) {
        int byteLen = (digits + 2) / 2;
        String numStr = String.format("%0" + (byteLen * 2 - 1) + "d", value);
        for (int i = 0; i < byteLen - 1; i++) {
            int hi = Character.getNumericValue(numStr.charAt(i * 2));
            int lo = Character.getNumericValue(numStr.charAt(i * 2 + 1));
            commarea[offset + i] = (byte) ((hi << 4) | lo);
        }
        // Last byte: last digit + positive sign (0x0C)
        int lastDigit = Character.getNumericValue(numStr.charAt(numStr.length() - 1));
        commarea[offset + byteLen - 1] = (byte) ((lastDigit << 4) | 0x0C);
    }

    private String padRight(String s, int length) {
        return String.format("%-" + length + "s", s);
    }

    /**
     * Exception thrown on CICS LINK failures.
     */
    public static class CicsLinkException extends Exception {
        public CicsLinkException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
