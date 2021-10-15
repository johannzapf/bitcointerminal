package de.johannzapf.bitcoin.terminal.objects;

import de.johannzapf.bitcoin.terminal.util.Util;
import lombok.*;

import static de.johannzapf.bitcoin.terminal.util.Constants.FEE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Transaction {

    @ToString.Include
    private String hash;

    private byte index;

    private String outputPubKey;

    @ToString.Include
    private long amount;

    public byte[] asByteArray(long outAmount){
        byte argA = index;
        byte[] argB = Util.hexStringToByteArray(hash);
        byte[] argC = Util.hexStringToByteArray(outputPubKey);
        byte[] argD = Util.hexStringToByteArray(Long.toHexString(amount - outAmount - FEE));

        byte[] res = new byte[66];
        res[0] = argA;
        int k = 0;
        for(int i = 1; i < 33; i++){
            res[i] = argB[k++];
        }
        k = 0;
        for(int i = 33; i < 58; i++){
            res[i] = argC[k++];
        }
        k = 8 - argD.length;
        for(int i = 58; i < 66; i++){
            res[i] = argD[k++];
        }

        return res;
    }

}
