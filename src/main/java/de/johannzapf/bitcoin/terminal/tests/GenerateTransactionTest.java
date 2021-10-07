package de.johannzapf.bitcoin.terminal.tests;

import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.SigningMessageTemplate;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Util;

import java.security.NoSuchAlgorithmException;


public class GenerateTransactionTest {

    private static String hexPrivateKey = "e9a39e81aff41f957e09af06fef3e83b84d3b56a95283324c05386ae4b8622d5";
    private static String publicKey = "044ef7fe1e6a0af7f9768fdccb73584ac5476536a1189ce4b9437c591d945701d42eaaa017b362f6bbab0c4b1ed912478f3e0e09ea345225dc6abaf8566ed25347";
    private static String address = "mxYt7PCYF9ePxnKh1JyK1PwpcggAXdn2Eh";

    public static void main(String[] args) throws NoSuchAlgorithmException, PaymentFailedException {
        SigningMessageTemplate smt = TransactionService.createSigningMessageTemplate(null, "", 0.0);
        System.out.println("To sign: \n" + smt.toPrettyString());
        System.out.println("\nOneliner: " + smt.toString());

        System.out.println("Double Hash: " + Util.bytesToHex(smt.doubleHash()));

        String sig = "3045022100f43370233e81468892a8ddb79594335e4aa33053a7728d6d3dd45c161db8df500220132ac43436be62aef38048f9a6dee826446c53d78c4639c3589be5b847c00e61";

        TransactionService.createTransaction(smt, Util.hexStringToByteArray(sig), Util.hexStringToByteArray(publicKey));


    }
}
