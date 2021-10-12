package de.johannzapf.bitcoin.terminal.tests;


import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.util.Util;

import java.math.BigInteger;

import static de.johannzapf.bitcoin.terminal.util.Util.bytesToHex;

public class UtilTest {

    private static final BigInteger N = new BigInteger("115792089237316195423570985008687907852837564279074904382605163141518161494337", 10);
    private static final BigInteger N2 = N.divide(new BigInteger("2", 10));

    private static final byte[] MAX_S = { (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0x5D, (byte) 0x57, (byte) 0x6E, (byte) 0x73, (byte) 0x57, (byte) 0xA4,
            (byte) 0x50, (byte) 0x1D, (byte) 0xDF, (byte) 0xE9, (byte) 0x2F, (byte) 0x46, (byte) 0x68, (byte) 0x1B,
            (byte) 0x20, (byte) 0xA0 };

    public static void main(String[] args) throws PaymentFailedException {
        String good = "30440220434435de3635f112062546d3ed295767a821cb936e560a09a5986c41dfc780ba02201a06d97094b0b3b31c620ee853bb59ae8ed1441943efee06bb39c65c5a686bdb";
        String bad = "3045022021dc2de24f22b1056d3189855b3dfffd4d1fb099b5b16d0ed11935ec759341a902210090d2acae476fbb7d0db492f0e21a25b40f65499c261566fbc90d32f07b14665e";

        byte[] goodS = Util.hexStringToByteArray(good);
        byte[] badS = Util.hexStringToByteArray(bad);

        //System.out.println("checkS(good): " + checkS(goodS));
        //System.out.println("checkS(bad): " + checkS(badS));
        System.out.println("checkSJC(good): " + checkSJC(goodS));
        System.out.println("checkSJC(bad): " + checkSJC(badS));
    }

    /**
     * Checks whether the s value of the given signature is smaller than N/2
     * @param signature
     * @return true if signature is OK, false otherwise
     */
    private static boolean checkS(byte[] signature){
        byte rlength = signature[3];
        byte slength = signature[5 + rlength];

        byte[] s = new byte[slength];
        int j = 0;
        for(int i = 6 + rlength; i < 6 + rlength + slength; i++){
            s[j++] = signature[i];
        }


        BigInteger bs = new BigInteger(bytesToHex(s), 16);
        boolean b =  bs.compareTo(N2) < 0;
        return b;
    }

    private static boolean checkSJC(byte[] signature){
        byte rlength = signature[3];
        byte slength = signature[5 + rlength];

        byte[] s = new byte[slength];
        int j = 0;
        for(int i = 6 + rlength; i < 6 + rlength + slength; i++){
            s[j++] = signature[i];
        }

        while (s[0] == 0x00) {
            byte[] newS = new byte[slength-1];
            for(int i = 0; i < slength-1; i++){
                newS[i] = s[i+1];
            }
            s = newS;
        }

        if(s.length < MAX_S.length){
            return true;
        }

        for(int i = 0; i < s.length; i++){
            if ((s[i] & 0xff) > (MAX_S[i] & 0xff)){
                return false;
            } else if ((s[i] & 0xff) < (MAX_S[i] & 0xff)){
                return true;
            }
        }

        return true;
    }
}
