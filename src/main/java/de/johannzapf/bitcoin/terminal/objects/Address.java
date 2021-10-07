package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class Address {

    private String address;
    private double confirmedBalance;
    private double unconfirmedBalance;

    private List<Transaction> transactions;


    public Transaction findProperTransaction(int amount) throws PaymentFailedException {
        Optional<Transaction> tx = transactions.stream().filter(t -> t.getAmount() > amount).findAny();
        if(tx.isPresent()){
            return tx.get();
        } else {
            throw new PaymentFailedException("No transaction with enough BTC found");
        }
    }
}
