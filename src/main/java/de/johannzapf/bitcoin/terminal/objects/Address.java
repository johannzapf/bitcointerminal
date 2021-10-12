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

    private List<Transaction> transactions;


    public List<Transaction> findProperTransactions(int amount) throws PaymentFailedException {
        List<Transaction> txs = new ArrayList<>();
        int am = 0;
        for(Transaction t : transactions){
            if(am < amount){
                txs.add(t);
                am += t.getAmount();
            }
        }
        return txs;
    }
}
