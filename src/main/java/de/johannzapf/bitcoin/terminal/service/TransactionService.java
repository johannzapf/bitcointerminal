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

public class TransactionService {

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
