package de.johannzapf.bitcoin.terminal.service;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static de.johannzapf.bitcoin.terminal.util.Util.satoshiToBTC;


public class AddressService {

    public static Address getAddressInfo(String btcAddress) throws PaymentFailedException {
        try {
            URL url = new URL(Constants.BLOCKCYPHER_API + "/addrs/" + btcAddress + "/full");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            JsonObject addr = Util.parseJSON(con);

            Address address = new Address();
            address.setAddress(addr.getString("address"));
            address.setConfirmedBalance(satoshiToBTC(addr.getInt("balance")));
            address.setUnconfirmedBalance(satoshiToBTC(addr.getInt("unconfirmed_balance")));

            List<Transaction> transactions = new ArrayList<>();

            JsonArray txs = addr.getJsonArray("txs");
            for(int i = 0; i < txs.size(); i++){
                JsonObject tx = txs.getJsonObject(i);
                JsonArray outputs = tx.getJsonArray("outputs");
                for(int j = 0; j < outputs.size(); j++){
                    JsonObject output = outputs.getJsonObject(j);
                    if(btcAddress.equals(output.getJsonArray("addresses").getString(0))){
                        transactions.add(new Transaction(tx.getString("hash"), (byte) j, output.getString("script"),
                                output.getInt("value")));
                    }
                }
            }
            address.setTransactions(transactions);

            return address;
        } catch (IOException e) {
            throw new PaymentFailedException("Blockcypher API Failed", e);
        }
    }


}
