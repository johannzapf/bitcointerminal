package de.johannzapf.bitcoin.terminal.service;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static de.johannzapf.bitcoin.terminal.util.Util.satoshiToBTC;


public class AddressService {

    public static Address getAddressInfo(String btcAddress) {
        try {
            URL url = new URL(Constants.BLOCKCYPHER_API + "/addrs/" + btcAddress +
                    "?unspentOnly=true&includeScript=true");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            JSONObject addr = Util.parseJSON(con);

            Address address = new Address();
            address.setAddress(addr.getString("address"));
            address.setConfirmedBalance(satoshiToBTC(addr.getInt("balance")));
            address.setUnconfirmedBalance(satoshiToBTC(addr.getInt("unconfirmed_balance")));
            address.setFinalBalance(satoshiToBTC(addr.getInt("final_balance")));

            List<Transaction> transactions = new ArrayList<>();

            if(addr.has("txrefs")) {
                JSONArray txs = addr.getJSONArray("txrefs");
                for (int i = 0; i < txs.length(); i++) {
                    JSONObject tx = txs.getJSONObject(i);
                    transactions.add(new Transaction(tx.getString("tx_hash"), (byte) tx.getInt("tx_output_n"),
                            tx.getString("script"), tx.getInt("value")));
                }
            }
            if(addr.has("unconfirmed_txrefs")){
                JSONArray txsu = addr.getJSONArray("unconfirmed_txrefs");
                for (int i = 0; i < txsu.length(); i++) {
                    JSONObject tx = txsu.getJSONObject(i);
                    transactions.add(new Transaction(tx.getString("tx_hash"), (byte) tx.getInt("tx_output_n"),
                            tx.getString("script"), tx.getInt("value")));
                }
            }
            address.setTransactions(transactions.stream().sorted(Comparator.comparingInt(Transaction::getAmount).reversed()).collect(Collectors.toList()));
            return address;
        } catch (IOException e) {
            throw new PaymentFailedException("Blockcypher API Failed", e);
        }
    }


}
