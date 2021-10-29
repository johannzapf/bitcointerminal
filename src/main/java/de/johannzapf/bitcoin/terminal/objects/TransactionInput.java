package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.Setter;

import static de.johannzapf.bitcoin.terminal.util.Util.*;

@Setter
public class TransactionInput {

    private byte[] previousTxHash;
    private byte[] previousOutputIndex = {0x00, 0x00, 0x00, 0x00};
    private int inScriptLength;
    private byte[] scriptSig;
    private byte[] sequence = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};


    public TransactionInput(UTXO inputTransaction){
        byte[] outputScriptPubKey1 = Util.hexStringToByteArray(inputTransaction.getOutputPubKey());
        this.previousTxHash = reverse(Util.hexStringToByteArray(inputTransaction.getHash()));
        this.previousOutputIndex[0] = inputTransaction.getIndex();
        this.inScriptLength = outputScriptPubKey1.length;
        this.scriptSig = outputScriptPubKey1;
    }

    @Override
    public String toString(){
        return bytesToHex(previousTxHash) +
                bytesToHex(previousOutputIndex) +
                toHex(inScriptLength) +
                bytesToHex(scriptSig) +
                bytesToHex(sequence);
    }

    public String toStringWithoutScript(){
        return bytesToHex(previousTxHash) +
                bytesToHex(previousOutputIndex) +
                toHex(0x00) +
                bytesToHex(sequence);
    }
}
