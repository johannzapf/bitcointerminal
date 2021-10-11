package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.Setter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static de.johannzapf.bitcoin.terminal.objects.SigningMessageTemplate.*;
import static de.johannzapf.bitcoin.terminal.util.Constants.FEE;
import static de.johannzapf.bitcoin.terminal.util.Util.*;
import static de.johannzapf.bitcoin.terminal.util.Util.bytesToHex;

@Setter
public class MultiSigningMessageTemplate {

    private byte[] version = {0x01, 0x00, 0x00, 0x00};

    //Inputs
    private byte numberOfInputs = 0x02;
    private byte[] previousTxHash1;
    private byte[] previousOutputIndex1 = {0x00, 0x00, 0x00, 0x00};
    private int inScriptLength1;
    private byte[] scriptSig1;
    private byte[] sequence1 = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    private byte[] previousTxHash2;
    private byte[] previousOutputIndex2 = {0x00, 0x00, 0x00, 0x00};
    private int inScriptLength2;
    private byte[] scriptSig2;
    private byte[] sequence2 = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

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


    public MultiSigningMessageTemplate(Transaction inputTransaction1, Transaction inputTransaction2, int outAmount,
                                       String destinationAddress, String senderAddress){

        byte[] pubKeyHash1 = getPubKeyHash(destinationAddress);
        byte[] pubKeyHash2 = getPubKeyHash(senderAddress);

        byte[] outputScriptPubKey1 = Util.hexStringToByteArray(inputTransaction1.getOutputPubKey());
        this.previousTxHash1 = reverse(Util.hexStringToByteArray(inputTransaction1.getHash()));
        this.previousOutputIndex1[0] = inputTransaction1.getIndex();
        this.inScriptLength1 = outputScriptPubKey1.length;
        this.scriptSig1 = outputScriptPubKey1;

        byte[] outputScriptPubKey2 = Util.hexStringToByteArray(inputTransaction2.getOutputPubKey());
        this.previousTxHash2 = reverse(Util.hexStringToByteArray(inputTransaction2.getHash()));
        this.previousOutputIndex2[0] = inputTransaction2.getIndex();
        this.inScriptLength2 = outputScriptPubKey2.length;
        this.scriptSig2 = outputScriptPubKey2;

        this.value1 = getValue(outAmount);
        this.outScriptLength1 = pubKeyHash1.length + 5;
        this.scriptPubKey1 = constructScriptPubKey(pubKeyHash1, pubKeyHash1.length + 5);
        this.value2 = getValue((inputTransaction1.getAmount() + inputTransaction2.getAmount()) - outAmount - FEE);
        this.outScriptLength2 = pubKeyHash2.length + 5;
        this.scriptPubKey2 = constructScriptPubKey(pubKeyHash2, pubKeyHash1.length + 5);
    }

    public byte[] doubleHash1() throws NoSuchAlgorithmException {
        String toHash = bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash1) +
                bytesToHex(previousOutputIndex1) +
                toHex(inScriptLength1) +
                bytesToHex(scriptSig1) +
                bytesToHex(sequence1) +
                bytesToHex(previousTxHash2) +
                bytesToHex(previousOutputIndex2) +
                toHex(0x00) +
                bytesToHex(sequence2) +
                toHexString(numberOfOutputs) +
                bytesToHex(value1) +
                toHex(outScriptLength1) +
                bytesToHex(scriptPubKey1) +
                bytesToHex(value2) +
                toHex(outScriptLength2) +
                bytesToHex(scriptPubKey2) +
                bytesToHex(locktime) +
                bytesToHex(sigHashCode);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha = digest.digest(Util.hexStringToByteArray(toHash));
        return digest.digest(sha);
    }

    public byte[] doubleHash2() throws NoSuchAlgorithmException {
        String toHash = bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash1) +
                bytesToHex(previousOutputIndex1) +
                toHex(0x00) +
                bytesToHex(sequence1) +
                bytesToHex(previousTxHash2) +
                bytesToHex(previousOutputIndex2) +
                toHex(inScriptLength2) +
                bytesToHex(scriptSig2) +
                bytesToHex(sequence2) +
                toHexString(numberOfOutputs) +
                bytesToHex(value1) +
                toHex(outScriptLength1) +
                bytesToHex(scriptPubKey1) +
                bytesToHex(value2) +
                toHex(outScriptLength2) +
                bytesToHex(scriptPubKey2) +
                bytesToHex(locktime) +
                bytesToHex(sigHashCode);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha = digest.digest(Util.hexStringToByteArray(toHash));
        return digest.digest(sha);
    }

    public String toString() {
        return bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash1) +
                bytesToHex(previousOutputIndex1) +
                toHex(inScriptLength1) +
                bytesToHex(scriptSig1) +
                bytesToHex(sequence1) +
                bytesToHex(previousTxHash2) +
                bytesToHex(previousOutputIndex2) +
                toHex(inScriptLength2) +
                bytesToHex(scriptSig2) +
                bytesToHex(sequence2) +
                toHexString(numberOfOutputs) +
                bytesToHex(value1) +
                toHex(outScriptLength1) +
                bytesToHex(scriptPubKey1) +
                bytesToHex(value2) +
                toHex(outScriptLength2) +
                bytesToHex(scriptPubKey2) +
                bytesToHex(locktime);
    }

}
