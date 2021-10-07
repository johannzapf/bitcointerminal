package de.johannzapf.bitcoin.terminal.service;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.SigningMessageTemplate;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static de.johannzapf.bitcoin.terminal.util.Util.BTCToSatoshi;

public class TransactionService {

    // Creates a Transaction that needs to be signed
    public static SigningMessageTemplate createSigningMessageTemplate(Address sender, String targetAddress, double amount) throws PaymentFailedException {
        Transaction tx = sender.findProperTransaction(BTCToSatoshi(amount));

        return new SigningMessageTemplate(tx.getHash(), tx.getIndex(), Util.hexStringToByteArray(tx.getOutputPubKey()), getValue(amount), targetAddress);
    }


    private static byte[] getValue(double amount) {
        byte[] value = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] a = Util.hexStringToByteArray(Integer.toHexString(BTCToSatoshi(amount)));
        int k = 0;
        for(int i = a.length-1; i >= 0; i--){
            value[k++] = a[i];
        }
        return value;
    }

    public static String createTransaction(SigningMessageTemplate smt, byte[] signature, byte[] pubKey) {
        //Construct scriptSig
        byte[] scriptSig = new byte[3 + signature.length + pubKey.length];
        scriptSig[0] = (byte) (signature.length + 1);

        for(int i = 0; i < signature.length; i++){
            scriptSig[i+1] = signature[i];
        }
        scriptSig[signature.length+1] = 0x01;
        scriptSig[signature.length+2] = (byte) pubKey.length;

        for(int i = 0; i < pubKey.length; i++){
            scriptSig[signature.length+3+i] = pubKey[i];
        }
        smt.setScriptSig(scriptSig);
        smt.setInScriptLength((short) scriptSig.length);

        return smt.toStringWithoutHashCode();
    }


    public static void decodeTransaction(String transaction){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.BLOCKCYPHER_API + "/txs/decode"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"tx\":\""+ transaction +"\"}"))
                .build();

        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("Response:" + response.statusCode() + ": " + response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
