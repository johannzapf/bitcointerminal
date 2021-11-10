package de.johannzapf.bitcoin.terminal.service;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.UTXO;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class TransactionService {

    /**
     * This method constructs the transaction parameters in the correct way for the card to understand them.
     * @param targetAddress
     * @param sAmount
     * @param inputTransactions
     * @return
     */
    public static byte[] constructTxParams(String targetAddress, long sAmount, List<UTXO> inputTransactions){
        byte[] targetAddressPKH = getPubKeyHash(targetAddress);
        byte[] amount = Util.hexStringToByteArray(toHex(sAmount));

        int inAmount = 0;
        for(UTXO t : inputTransactions){
            inAmount += t.getAmount();
        }
        byte[] change = Util.hexStringToByteArray(Util.toHex(inAmount - sAmount - calculateFee(inputTransactions.size())));

        int arraySize = 37 + 58 * inputTransactions.size();
        byte[] params = new byte[arraySize];

        int k = 0;
        for(int i = 0; i < 20; i++){
            params[i] = targetAddressPKH[k++];
        }
        k = 0;
        for(int i = 20 + (8-amount.length); i < 28; i++){
            params[i] = amount[k++];
        }
        k = 0;
        for(int i = 28 + (8-change.length); i < 36; i++){
            params[i] = change[k++];
        }

        params[36] = (byte) inputTransactions.size();

        for(int j = 0; j < inputTransactions.size(); j++){
            byte[] inputTransaction = inputTransactions.get(j).asByteArray();
            k = 0;
            for(int i = 37; i < 95; i++){
                params[i + j * 58] = inputTransaction[k++];
            }
        }
        return params;
    }

    /**
     * Takes a raw transaction (as a hex string) and publishes it to the blockchain via the BlockCypher API.
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
