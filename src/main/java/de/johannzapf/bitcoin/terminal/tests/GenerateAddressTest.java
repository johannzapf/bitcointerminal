package de.johannzapf.bitcoin.terminal.tests;

import de.johannzapf.bitcoin.terminal.util.Util;
import org.bitcoinj.core.Base58;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

import static de.johannzapf.bitcoin.terminal.util.Util.bytesToHex;

public class GenerateAddressTest {

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, UnsupportedEncodingException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());

        /*KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyGen.initialize(ecSpec);
        KeyPair kp = keyGen.generateKeyPair();
        ECPrivateKey pvt = (ECPrivateKey) kp.getPrivate();
        System.out.println("Private Key: " + adjustTo64(pvt.getS().toString(16)).toUpperCase());

        ECPublicKey epub = (ECPublicKey)kp.getPublic();
        ECPoint pt = epub.getW();
        String sx = adjustTo64(pt.getAffineX().toString(16)).toUpperCase();
        String sy = adjustTo64(pt.getAffineY().toString(16)).toUpperCase();*/
        String bcPub = "047eec7d809959d50e2f16335b9dd98c4a391aa08d50579e146f81c861e9994f5a3c2b15ce7c1253d24d6ebadfe90f7680f7a6210610b378e99424d6a894c4ed65";
        System.out.println("Public Key: " + bcPub);

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] s1 = sha.digest(Util.hexStringToByteArray(bcPub));
        MessageDigest rmd = MessageDigest.getInstance("RipeMD160", "BC");
        byte[] r1 = rmd.digest(s1);

        //Add Network Byte (0x00 for Mainnet, 0x6F for Testnet)
        byte[] r2 = new byte[r1.length + 1];
        r2[0] = 0x6f;
        for (int i = 0 ; i < r1.length ; i++) r2[i+1] = r1[i];

        byte[] s2 = sha.digest(r2);
        byte[] s3 = sha.digest(s2);
        byte[] a1 = new byte[25];
        for (int i = 0 ; i < r2.length ; i++) a1[i] = r2[i];
        for (int i = 0 ; i < 4 ; i++) a1[21 + i] = s3[i];

        System.out.println("Address: " + Base58.encode(a1));
    }



    static private String adjustTo64(String s) {
        switch(s.length()) {
            case 62: return "00" + s;
            case 63: return "0" + s;
            case 64: return s;
            default:
                throw new IllegalArgumentException("not a valid key: " + s);
        }
    }
}
