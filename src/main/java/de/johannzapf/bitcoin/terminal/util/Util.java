package de.johannzapf.bitcoin.terminal.util;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class Util {

    public static double satoshiToBTC(double satoshi){
        return satoshi/100000000;
    }

    public static long BTCToSatoshi(double btc){
        return Math.round(btc*100000000);
    }

    public static JSONObject parseJSON(HttpURLConnection conn) throws IOException {
        String res = "";
        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String s;
        while((s = r.readLine()) != null){
            res += s;
        }
        return new JSONObject(res);
    }

    public static String toHex(int i){
        String s = Integer.toHexString(i);
        if(s.length() % 2 != 0){
            return "0" + s;
        }
        return s;
    }

    public static String toHex(long i){
        String s = Long.toHexString(i);
        if(s.length() % 2 != 0){
            return "0" + s;
        }
        return s;
    }

    public static String toHexString(short b){
        if(b <= 0x09){
            return "0" + b;
        } else {
            return String.valueOf(b);
        }
    }

    public static byte[] reverse(byte[] a){
        byte[] reversed = new byte[a.length];
        for(int i = 0; i < a.length; i++){
            reversed[i] = a[a.length-1-i];
        }
        return reversed;
    }

    public static String hexToAscii(byte[] hex){
        StringBuffer sb = new StringBuffer();
        for(byte b : hex){
            sb.append((char) b);
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

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
