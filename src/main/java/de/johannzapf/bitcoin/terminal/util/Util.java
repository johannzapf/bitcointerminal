package de.johannzapf.bitcoin.terminal.util;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import static de.johannzapf.bitcoin.terminal.Settings.FEE;


public class Util {

    public static double satoshiToBTC(double satoshi){
        return satoshi/100000000;
    }

    public static int BTCToSatoshi(double btc){
        return (int) Math.round(btc*100000000);
    }

    /**
     * Calculates a fee from the amount of inputs in a transaction.
     * Makes use of the fact that, in our case, a transaction is about (80 + 180 * amount of inputs) bytes long.
     * @param inputsAmount
     * @return
     */
    public static long calculateFee(int inputsAmount){
        int transactionSize = inputsAmount * 180 + 80;
        return transactionSize * FEE;
    }

    /**
     * Parses the answer from a HttpURLConnection into a JSONObject.
     * @param conn
     * @return
     * @throws IOException
     */
    public static JSONObject parseJSON(HttpURLConnection conn) throws IOException {
        String res = "";
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String s;
        while((s = r.readLine()) != null){
            res += s;
        }
        return new JSONObject(res);
    }

    /**
     * Converts an int value into a hex string (and appends leading zeros).
     * @param i
     * @return
     */
    public static String toHex(int i){
        String s = Integer.toHexString(i);
        if(s.length() % 2 != 0){
            return "0" + s;
        }
        return s;
    }

    /**
     * Converts a short value to a hex string (and appends leading zeros).
     * @param b
     * @return
     */
    public static String toHexString(short b){
        if(b <= 0x09){
            return "0" + b;
        } else {
            return String.valueOf(b);
        }
    }

    /**
     * Reverses a byte array
     * @param a
     * @return
     */
    public static byte[] reverse(byte[] a){
        byte[] reversed = new byte[a.length];
        for(int i = 0; i < a.length; i++){
            reversed[i] = a[a.length-1-i];
        }
        return reversed;
    }

    /**
     * Converts an array of bytes into a string ascii representation.
     * @param hex
     * @return
     */
    public static String hexToAscii(byte[] hex){
        StringBuffer sb = new StringBuffer();
        for(byte b : hex){
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * Converts a hex string to an array of bytes.
     * @param s
     * @return
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Converts an array of bytes to a hex string.
     * Taken from stackoverflow (https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java).
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
