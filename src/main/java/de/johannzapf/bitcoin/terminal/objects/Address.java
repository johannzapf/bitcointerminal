package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
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


    public List<UTXO> findProperUTXOs(long amount) throws PaymentFailedException {
        List<UTXO> txs = new ArrayList<>();
        long am = 0;
        for(UTXO t : UTXOs){
            if(am < amount){
                txs.add(t);
                am += t.getAmount();
            }
        }
        return txs;
    }
}
