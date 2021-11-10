package de.johannzapf.bitcoin.terminal.service;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.*;
import de.johannzapf.bitcoin.terminal.util.Constants;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class TransactionService {

    /**
     * Takes a Transaction object and the signatures by the card and creates the final transaction.
     * @param tx
     * @param signatures
     * @param pubKey
     * @return
     */
    public static String createTransaction(Transaction tx, List<byte[]> signatures,
                                           byte[] pubKey){
        List<TransactionInput> inputs = tx.getInputs();
        if(inputs.size() != signatures.size()){
            throw new PaymentFailedException("Signature count does not match");
        }
        int i = 0;
        for(byte[] signature : signatures){
            byte[] scriptSig = getScriptSig(signature, pubKey);
            inputs.get(i).setScriptSig(scriptSig);
            inputs.get(i++).setInScriptLength(scriptSig.length);
        }

        return tx.toString();
    }

    /**
     * Constrcuts a ScriptSig from a signature and a public key
     * @param signature
     * @param pubKey
     * @return
     */
    private static byte[] getScriptSig(byte[] signature, byte[] pubKey){
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
        return scriptSig;
    }

    /**
     * Broadcasts a given raw bitcoin transaction (as a hex string) to the blockchain via the BlockCypher API.
     * @param transaction
     * @return
     */
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
