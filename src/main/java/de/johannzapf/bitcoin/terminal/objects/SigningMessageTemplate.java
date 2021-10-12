package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.Setter;
import org.bitcoinj.core.Address;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static de.johannzapf.bitcoin.terminal.util.Constants.FEE;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

@Setter
public class SigningMessageTemplate {

    private byte[] version = {0x01, 0x00, 0x00, 0x00};

    //Input
    private byte numberOfInputs = 0x01;
    private byte[] previousTxHash;
    private byte[] previousOutputIndex = {0x00, 0x00, 0x00, 0x00};
    private int inScriptLength;
    private byte[] scriptSig;
    private byte[] sequence = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    //Outputs
    private byte numberOfOutputs = 0x02;
    private byte[] value1;
    private int outScriptLength1;
    private byte[] scriptPubKey1;
    private byte[] value2;
    private int outScriptLength2;
    private byte[] scriptPubKey2;

    private byte[] locktime = {0x00, 0x00, 0x00, 0x00};
    private byte[] sigHashCode = {0x01, 0x00, 0x00, 0x00};


    public SigningMessageTemplate(Transaction inputTransaction, int outAmount, String destinationAddress,
                                  String senderAddress){
        byte[] pubKeyHash1 = getPubKeyHash(destinationAddress);
        byte[] pubKeyHash2 = getPubKeyHash(senderAddress);
        this.previousTxHash = reverse(Util.hexStringToByteArray(inputTransaction.getHash()));
        this.previousOutputIndex[0] = inputTransaction.getIndex();
        byte[] outputScriptPubKey = Util.hexStringToByteArray(inputTransaction.getOutputPubKey());
        this.inScriptLength = outputScriptPubKey.length;
        this.scriptSig = outputScriptPubKey;
        this.value1 = getValue(outAmount);

        this.outScriptLength1 = pubKeyHash1.length + 5;
        this.scriptPubKey1 = constructScriptPubKey(pubKeyHash1, pubKeyHash1.length + 5);
        this.value2 = getValue(inputTransaction.getAmount() - outAmount - FEE);
        this.outScriptLength2 = pubKeyHash2.length + 5;
        this.scriptPubKey2 = constructScriptPubKey(pubKeyHash2, pubKeyHash1.length + 5);
    }

    public byte[] doubleHash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha = digest.digest(Util.hexStringToByteArray(this.toString()));
        return digest.digest(sha);
    }

    public String toPrettyString() {
        return bytesToHex(version) + "\n" +
                toHexString(numberOfInputs) + "\n" +
                bytesToHex(previousTxHash) + "\n" +
                bytesToHex(previousOutputIndex) + "\n" +
                toHex(inScriptLength) + "\n" +
                bytesToHex(scriptSig) + "\n" +
                bytesToHex(sequence) + "\n" +
                toHexString(numberOfOutputs) + "\n" +
                bytesToHex(value1) + "\n" +
                toHex(outScriptLength1) + "\n" +
                bytesToHex(scriptPubKey1) + "\n" +
                bytesToHex(value2) + "\n" +
                toHex(outScriptLength2) + "\n" +
                bytesToHex(scriptPubKey2) + "\n" +
                bytesToHex(locktime) + "\n" +
                bytesToHex(sigHashCode);
    }

    public String toPrettyStringWithoutHashCode() {
        return bytesToHex(version) + "\n" +
                toHexString(numberOfInputs) + "\n" +
                bytesToHex(previousTxHash) + "\n" +
                bytesToHex(previousOutputIndex) + "\n" +
                toHex(inScriptLength) + "\n" +
                bytesToHex(scriptSig) + "\n" +
                bytesToHex(sequence) + "\n" +
                toHexString(numberOfOutputs) + "\n" +
                bytesToHex(value1) + "\n" +
                toHex(outScriptLength1) + "\n" +
                bytesToHex(scriptPubKey1) + "\n" +
                bytesToHex(value2) + "\n" +
                toHex(outScriptLength2) + "\n" +
                bytesToHex(scriptPubKey2) + "\n" +
                bytesToHex(locktime);
    }

    public String toString() {
        return bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash) +
                bytesToHex(previousOutputIndex) +
                toHex(inScriptLength) +
                bytesToHex(scriptSig) +
                bytesToHex(sequence) +
                toHexString(numberOfOutputs) +
                bytesToHex(value1) +
                toHex(outScriptLength1) +
                bytesToHex(scriptPubKey1) +
                bytesToHex(value2) +
                toHex(outScriptLength2) +
                bytesToHex(scriptPubKey2) +
                bytesToHex(locktime) +
                bytesToHex(sigHashCode);
    }

    public String toStringWithoutHashCode() {
        return bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash) +
                bytesToHex(previousOutputIndex) +
                toHex(inScriptLength) +
                bytesToHex(scriptSig) +
                bytesToHex(sequence) +
                toHexString(numberOfOutputs) +
                bytesToHex(value1) +
                toHex(outScriptLength1) +
                bytesToHex(scriptPubKey1) +
                bytesToHex(value2) +
                toHex(outScriptLength2) +
                bytesToHex(scriptPubKey2) +
                bytesToHex(locktime);
    }

    public static byte[] getValue(int satoshi) {
        byte[] value = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        String hex = Integer.toHexString(satoshi);
        if(hex.length() % 2 != 0){
            hex = "0" + hex;
        }
        byte[] a = Util.hexStringToByteArray(hex);
        int k = 0;
        for(int i = a.length-1; i >= 0; i--){
            value[k++] = a[i];
        }
        return value;
    }

    public static byte[] constructScriptPubKey(byte[] pubKeyHash, int scriptLength){
        byte[] scriptPubKey = new byte[scriptLength];
        scriptPubKey[0] = (byte) 0x76;
        scriptPubKey[1] = (byte) 0xa9;
        scriptPubKey[2] = (byte) 0x14;
        for(byte b = 0; b < pubKeyHash.length; b++){
            scriptPubKey[3+b] = pubKeyHash[b];
        }
        scriptPubKey[scriptLength-2] = (byte) 0x88;
        scriptPubKey[scriptLength-1] = (byte) 0xac;

        return scriptPubKey;
    }

    public static byte[] getPubKeyHash(String address){
        return Address.fromString(Constants.netParams, address).getHash();
    }

}
