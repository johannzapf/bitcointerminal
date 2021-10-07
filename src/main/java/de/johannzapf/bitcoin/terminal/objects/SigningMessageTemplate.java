package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.Setter;
import org.bitcoinj.core.Address;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    //Output
    private byte numberOfOutputs = 0x01;
    private byte[] value;
    private int outScriptLength;
    private byte[] scriptPubKey;

    private byte[] locktime = {0x00, 0x00, 0x00, 0x00};
    private byte[] sigHashCode = {0x01, 0x00, 0x00, 0x00};


    public SigningMessageTemplate(String txHash, byte previousOutputIndex, byte[] outputScriptPubKey,
                                  byte[] amount, String destinationAddress){
        byte[] pubKeyHash = getPubKeyHash(destinationAddress);
        this.previousTxHash = reverse(Util.hexStringToByteArray(txHash));
        this.previousOutputIndex[0] = previousOutputIndex;
        this.inScriptLength = outputScriptPubKey.length;
        this.scriptSig = outputScriptPubKey;
        this.value = amount;
        this.outScriptLength = pubKeyHash.length + 5;
        this.scriptPubKey = constructScriptPubKey(pubKeyHash, pubKeyHash.length + 5);
    }

    public byte[] doubleHash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha = digest.digest(Util.hexStringToByteArray(this.toString()));
        return digest.digest(sha);
    }

    public byte[] singleHash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(Util.hexStringToByteArray(this.toString()));
    }

    public String toPrettyString() {
        return bytesToHex(version) + "\n" +
                toHexString(numberOfInputs) + "\n" +
                bytesToHex(previousTxHash) + "\n" +
                bytesToHex(previousOutputIndex) + "\n" +
                Integer.toHexString(inScriptLength) + "\n" +
                bytesToHex(scriptSig) + "\n" +
                bytesToHex(sequence) + "\n" +
                toHexString(numberOfOutputs) + "\n" +
                bytesToHex(value) + "\n" +
                Integer.toHexString(outScriptLength) + "\n" +
                bytesToHex(scriptPubKey) + "\n" +
                bytesToHex(locktime) + "\n" +
                bytesToHex(sigHashCode);
    }

    public String toPrettyStringWithoutHashCode() {
        return bytesToHex(version) + "\n" +
                toHexString(numberOfInputs) + "\n" +
                bytesToHex(previousTxHash) + "\n" +
                bytesToHex(previousOutputIndex) + "\n" +
                Integer.toHexString(inScriptLength) + "\n" +
                bytesToHex(scriptSig) + "\n" +
                bytesToHex(sequence) + "\n" +
                toHexString(numberOfOutputs) + "\n" +
                bytesToHex(value) + "\n" +
                Integer.toHexString(outScriptLength) + "\n" +
                bytesToHex(scriptPubKey) + "\n" +
                bytesToHex(locktime);
    }

    public String toString() {
        return bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash) +
                bytesToHex(previousOutputIndex) +
                Integer.toHexString(inScriptLength) +
                bytesToHex(scriptSig) +
                bytesToHex(sequence) +
                toHexString(numberOfOutputs) +
                bytesToHex(value) +
                Integer.toHexString(outScriptLength) +
                bytesToHex(scriptPubKey) +
                bytesToHex(locktime) +
                bytesToHex(sigHashCode);
    }

    public String toStringWithoutHashCode() {
        return bytesToHex(version) +
                toHexString(numberOfInputs) +
                bytesToHex(previousTxHash) +
                bytesToHex(previousOutputIndex) +
                Integer.toHexString(inScriptLength) +
                bytesToHex(scriptSig) +
                bytesToHex(sequence) +
                toHexString(numberOfOutputs) +
                bytesToHex(value) +
                Integer.toHexString(outScriptLength) +
                bytesToHex(scriptPubKey) +
                bytesToHex(locktime);
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
