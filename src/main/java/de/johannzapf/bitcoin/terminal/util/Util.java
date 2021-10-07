package de.johannzapf.bitcoin.terminal.util;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class Util {

    public static double satoshiToBTC(double satoshi){
        return satoshi/100000000;
    }

    public static int BTCToSatoshi(double btc){
        return (int) (btc*100000000);
    }

    public static JsonObject parseJSON(HttpURLConnection conn) throws IOException {
        JsonReader jsonReader = Json.createReader(new InputStreamReader(conn.getInputStream()));
        JsonObject obj = jsonReader.readObject();
        jsonReader.close();
        return obj;
    }

    public static byte toHex(byte b){
        byte[] array = new byte[1];
        array[0] = b;
        String s = Util.bytesToHex(array);
        return Byte.parseByte(s, 16);
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
