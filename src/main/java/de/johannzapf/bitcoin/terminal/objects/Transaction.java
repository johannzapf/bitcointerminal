package de.johannzapf.bitcoin.terminal.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String hash;
    private byte index;
    private String outputPubKey;
    private double amount;

}
