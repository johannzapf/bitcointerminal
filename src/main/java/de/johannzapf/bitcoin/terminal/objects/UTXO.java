package de.johannzapf.bitcoin.terminal.objects;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class UTXO {

    @ToString.Include
    private String hash;

    private byte index;

    private String outputPubKey;

    @ToString.Include
    private int amount;

}
