package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Util;
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
    private long amount;

    /**
     * This method returns this UTXO in a form our card can understand.
     * @return
     */
    public byte[] asByteArray(){
        byte[] prevTxHash = Util.hexStringToByteArray(hash);
        byte[] outputPubkey = Util.hexStringToByteArray(outputPubKey);

        byte[] res = new byte[58];
        res[0] = index;
        int k = 0;
        for(int i = 1; i < 33; i++){
            res[i] = prevTxHash[k++];
        }
        k = 0;
        for(int i = 33; i < 58; i++){
            res[i] = outputPubkey[k++];
        }
        return res;
    }

}
