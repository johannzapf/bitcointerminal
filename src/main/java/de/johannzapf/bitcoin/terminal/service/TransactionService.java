package de.johannzapf.bitcoin.terminal.service;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.SigningMessageTemplate;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.util.Constants;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransactionService {

    // Creates a Transaction that needs to be signed
    public static SigningMessageTemplate createSigningMessageTemplate(Address sender, String targetAddress, int amount,
                                                                      Transaction inputTransaction) {
        return new SigningMessageTemplate(inputTransaction, amount, targetAddress, sender.getAddress());

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

    public static String broadcastTransaction(String transaction) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.BLOCKCYPHER_API + "/txs/push"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"tx\":\""+ transaction +"\"}"))
                .build();

        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 201){
                return new JSONObject(response.body()).getJSONObject("tx").getString("hash");
            } else {
                throw new PaymentFailedException("Failed to broadcast Transaction: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new PaymentFailedException("Blockcypher API Failed", e);
        }
    }
}
