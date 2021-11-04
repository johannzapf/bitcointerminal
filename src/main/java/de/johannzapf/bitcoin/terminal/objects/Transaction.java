package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.Getter;
import lombok.Setter;
import org.bitcoinj.core.Address;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.johannzapf.bitcoin.terminal.util.Util.*;
import static de.johannzapf.bitcoin.terminal.util.Util.bytesToHex;

@Setter
@Getter
public class Transaction {

    private byte[] version = {0x01, 0x00, 0x00, 0x00};

    //Inputs
    private byte numberOfInputs;
    private List<TransactionInput> inputs;

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


    public Transaction(List<UTXO> inputTransactions, int outAmount,
                       String destinationAddress, String senderAddress){

        byte[] pubKeyHash1 = getPubKeyHash(destinationAddress);
        byte[] pubKeyHash2 = getPubKeyHash(senderAddress);

        this.numberOfInputs = (byte) inputTransactions.size();
        this.inputs = inputTransactions.stream().map(TransactionInput::new).collect(Collectors.toList());

        this.value1 = getValue(outAmount);
        this.outScriptLength1 = pubKeyHash1.length + 5;
        this.scriptPubKey1 = constructScriptPubKey(pubKeyHash1, pubKeyHash1.length + 5);

        int inAmount = 0;
        for(UTXO t : inputTransactions){
            inAmount += t.getAmount();
        }
        this.value2 = getValue(inAmount - outAmount - Util.calculateFee(inputTransactions.size()));
        this.outScriptLength2 = pubKeyHash2.length + 5;
        this.scriptPubKey2 = constructScriptPubKey(pubKeyHash2, pubKeyHash1.length + 5);
    }

    public List<byte[]> toSign() throws NoSuchAlgorithmException {
        List<byte[]> hashes = new ArrayList<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for(int i = 0; i < this.inputs.size(); i++) {
            StringBuilder toHash = new StringBuilder(bytesToHex(version) +
                    toHexString(numberOfInputs));

            for (TransactionInput ti : this.inputs) {
                if (inputs.indexOf(ti) == i) {
                    toHash.append(ti.toString());
                } else {
                    toHash.append(ti.toStringWithoutScript());
                }
            }

            toHash.append(toHexString(numberOfOutputs))
                    .append(bytesToHex(value1))
                    .append(toHex(outScriptLength1))
                    .append(bytesToHex(scriptPubKey1))
                    .append(bytesToHex(value2))
                    .append(toHex(outScriptLength2))
                    .append(bytesToHex(scriptPubKey2))
                    .append(bytesToHex(locktime))
                    .append(bytesToHex(sigHashCode));

            byte[] sha = digest.digest(Util.hexStringToByteArray(toHash.toString()));
            hashes.add(digest.digest(sha));
        }

        return hashes;
    }


    public String toString() {
        StringBuilder in = new StringBuilder();
        for(TransactionInput ti : inputs){
            in.append(ti.toString());
        }

        return bytesToHex(version) +
                toHexString(numberOfInputs) +
                in.toString() +
                toHexString(numberOfOutputs) +
                bytesToHex(value1) +
                toHex(outScriptLength1) +
                bytesToHex(scriptPubKey1) +
                bytesToHex(value2) +
                toHex(outScriptLength2) +
                bytesToHex(scriptPubKey2) +
                bytesToHex(locktime);
    }

    public static byte[] getValue(long satoshi) {
        byte[] value = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        String hex = Long.toHexString(satoshi);
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
