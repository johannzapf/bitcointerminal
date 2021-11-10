package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Address {

    private String address;
    private double confirmedBalance;
    private double unconfirmedBalance;
    private double finalBalance;

    private List<UTXO> UTXOs;


    /**
     * Goes through all UTXOs and returns the minimum set of UTXOs needed to spent the given amount (including fees).
     * @param amount
     * @return
     */
    public List<UTXO> findProperUTXOs(long amount) {
        List<UTXO> txs = new ArrayList<>();
        long am = 0;
        for(UTXO t : UTXOs){
            if(am < amount + Util.calculateFee(txs.size())){
                txs.add(t);
                am += t.getAmount();
            }
        }
        return txs;
    }
}
