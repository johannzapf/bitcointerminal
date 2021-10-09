package de.johannzapf.bitcoin.terminal;

import apdu4j.pcsc.TerminalManager;
import apdu4j.pcsc.terminals.LoggingCardTerminal;
import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.SigningMessageTemplate;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.service.AddressService;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import org.bitcoinj.core.Base58;

import javax.smartcardio.*;
import java.math.BigInteger;
import java.security.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class Application {

    private static final BigInteger N = new BigInteger("115792089237316195423570985008687907852837564279074904382605163141518161494337", 10);
    private static final BigInteger N2 = N.divide(new BigInteger("2", 10));

    private static double amount = 0.0046;
    private static String targetAddress = "mzjF1AQLZek45yhhwWGuTCNxHCafSbF6iS";

    private static Scanner scanner = new Scanner(System.in);
    private static DecimalFormat format = new DecimalFormat("#0.00");

    public static void main(String[] args) throws CardException, NoSuchAlgorithmException {
        System.out.println("-------- Initializing Payment Terminal --------");
        CardTerminal terminal = initializeTerminal();
        System.out.println("Terminal connected: " + terminal.getName());
        //readPaymentParams();

        System.out.println("-------------- Connecting to Card --------------");

        System.out.println("Waiting for Card...");
        if(!terminal.waitForCardPresent(CARD_READER_TIMEOUT)){
            System.out.println("ERROR: Terminal timeout");
            return;
        }

        long start = System.nanoTime();

        CardChannel channel = openCardChannel(terminal);
        if(!selectApplet(channel)){
            System.out.println("ERROR: No Bitcoin Wallet Applet on this Card");
            return;
        }
        System.out.println("Card successfully connected.");

        version(channel);
        connectionMode(channel);

        if(!cardStatus(channel)){
            System.out.println("Bitcoin Wallet on this card is not initialized. Would you like to initialize it (y/n)?");
            if(scanner.nextLine().equals("y")){
                initializeWallet(channel);
            } else {
                return;
            }
        }

        System.out.println("------------ Payment Process Start ------------");
        String btcAddress = getAddress(channel);
        System.out.println(">> Address: " + btcAddress);
        byte[] pubKey = getPubKey(channel);
        System.out.println(">> Public Key: " + bytesToHex(pubKey));

        Address address = AddressService.getAddressInfo(btcAddress);
        System.out.println("Available Balance in this Wallet: " + address.getFinalBalance() +
                " BTC (confirmed: " + address.getConfirmedBalance() + " BTC)");
        if(address.getFinalBalance() + satoshiToBTC(FEE) < amount) {
            System.out.println("ERROR: The funds in this wallet are not sufficient for this transaction.");
            return;
        }

        System.out.println("Creating Transaction...");
        List<Transaction> txs = address.findProperTransactions(BTCToSatoshi(amount) + FEE);
        String finalTransaction;

        if(txs.size() == 1){
            SigningMessageTemplate smt = TransactionService.createSigningMessageTemplate(address, targetAddress, BTCToSatoshi(amount), txs.get(0));

            System.out.print("Sending to Smartcard for approval..");

            byte[] signature;
            do {
                signature = sendTransaction(channel, smt.doubleHash());
                System.out.print(".");
            } while(!checkS(signature));
            long end = System.nanoTime();
            double elapsed = ((double)(end-start))/1_000_000_000;
            System.out.println("\nYou can remove your card (" + format.format(elapsed) + " Seconds)");

            finalTransaction = TransactionService.createTransaction(smt, signature, pubKey);
            System.out.println("FINAL TRANSACTION: " + finalTransaction);

        } else {
            throw new PaymentFailedException("No support yet for transactions with multiple inputs");
        }

        System.out.println("Broadcast Transaction to P2P Network (y/n)?");
        if(scanner.nextLine().equals("y")){
            String hash = TransactionService.broadcastTransaction(finalTransaction);
            System.out.println("Transaction with hash \"" + hash + "\" was successfully broadcast.");
        }
    }

    /**
     * Checks whether the s value of the given signature is smaller than N/2
     * @param signature
     * @return true if signature is OK, false otherwise
     */
    private static boolean checkS(byte[] signature){

        byte rlength = signature[3];
        byte slength = signature[5 + rlength];

        byte[] s = new byte[slength];
        int j = 0;
        for(int i = 6 + rlength; i < 6 + rlength + slength; i++){
            s[j++] = signature[i];
        }

        BigInteger bs = new BigInteger(bytesToHex(s), 16);

        return bs.compareTo(N2) < 0;

        /*if(bs.compareTo(n2) > 0){
            System.err.println("Fixing Signature...");
            System.out.println("Old signature: " + bytesToHex(signature));
            String newbs = N.subtract(bs).toString(16);
            while(newbs.length() < 64){
                newbs = "0" + newbs;
            }
            System.out.println("NEW S: " + newbs);
            byte[] newS = hexStringToByteArray(N.subtract(bs).toString(16));



            if(newS.length == s.length){
                int k = 0;
                for(int i = 6 + rlength; i < 6 + rlength + slength; i++){
                    signature[i] = newS[k++];
                }
            } else {
                System.err.println("Signature length has changed");
                int diff = s.length - newS.length;
                byte[] newSig = new byte[signature.length - diff];
                for(int i = 0; i < 5 + rlength; i++){
                    newSig[i] = signature[i];
                }
                newSig[5+rlength] = (byte) (slength-diff);
                int k = 0;
                for(int i = 6 + rlength; i < 6 + rlength + newS.length; i++){
                    newSig[i] = newS[k++];
                }
                System.out.println("New signature: " + bytesToHex(newSig));
                return newSig;
            }
        }

        return signature;*/
    }


    private static byte[] sendTransaction(CardChannel channel, byte[] transaction) throws CardException{
        CommandAPDU pay = new CommandAPDU(CLA, INS_PAY, 0x00, 0x00, transaction);
        ResponseAPDU res = channel.transmit(pay);
        if(isSuccessful(res)){
            byte[] data = res.getData();
            return data;
        } else {
            throw new PaymentFailedException("Transaction returned " + Arrays.toString(res.getData()));
        }

    }

    private static void version(CardChannel channel) throws CardException {
        CommandAPDU version = new CommandAPDU(CLA, INS_VERSION, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(version);
        if(isSuccessful(res)){
            System.out.println(">> Version: " + hexToAscii(res.getData()));
        } else {
            throw new PaymentFailedException("Card Version returned " + Arrays.toString(res.getData()));
        }
    }

    private static void connectionMode(CardChannel channel) throws CardException {
        CommandAPDU conn = new CommandAPDU(CLA, INS_CONN_MODE, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(conn);
        if(isSuccessful(res)){
            if(res.getData()[0] == (byte) 0){
                System.out.println("Card is connected physically");
            } else {
                System.out.println("Card is connected via NFC");
            }
        } else {
            throw new PaymentFailedException("Card Hello returned " + Arrays.toString(res.getData()));
        }
    }

    private static boolean cardStatus(CardChannel channel) throws CardException {
        CommandAPDU status = new CommandAPDU(CLA, INS_STATUS, 0x00, 0x00, 0x01);
        ResponseAPDU res = channel.transmit(status);
        if(isSuccessful(res)){
            byte s = res.getData()[0];
            System.out.println(">> Status: " + s);
            return s == 1;
        } else {
            throw new PaymentFailedException("Card Status returned " + Arrays.toString(res.getData()));
        }
    }

    private static void initializeWallet(CardChannel channel) throws CardException {
        CommandAPDU init = new CommandAPDU(CLA, INS_INIT, P1_TESTNET, 0x00);
        if(!isSuccessful(channel.transmit(init))){
            throw new PaymentFailedException("Error initializing Wallet");
        }
    }

    private static String getAddress(CardChannel channel) throws CardException {
        CommandAPDU addr = new CommandAPDU(CLA, INS_GET_ADDR, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(addr);
        if(isSuccessful(res)){
            return Base58.encode(res.getData());
        } else {
            throw new PaymentFailedException("Error getting address");
        }
    }

    private static byte[] getPubKey(CardChannel channel) throws CardException {
        CommandAPDU pubKey = new CommandAPDU(CLA, INS_GET_PUBKEY, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(pubKey);
        if(isSuccessful(res)){
            return res.getData();
        } else {
            throw new PaymentFailedException("Error getting address");
        }
    }

    private static byte[] getPrivKey(CardChannel channel) throws CardException {
        CommandAPDU privKey = new CommandAPDU(CLA, INS_GET_PRIVKEY, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(privKey);
        if(isSuccessful(res)){
            return res.getData();
        } else {
            throw new PaymentFailedException("Error getting address");
        }
    }

    private static CardTerminal initializeTerminal() throws NoSuchAlgorithmException, CardException {
        TerminalManager.fixPlatformPaths();
        TerminalFactory factory = TerminalFactory.getInstance("PC/SC", null);
        CardTerminal terminal = factory.terminals().list().get(0);
        LoggingCardTerminal lct = LoggingCardTerminal.getInstance(terminal);
        return DEBUG ? lct : terminal;
    }

    private static void readPaymentParams(){
        System.out.println("Payment Amount (BTC): ");
        amount = Double.parseDouble(scanner.nextLine());
        System.out.println("Target Bitcoin Address: ");
        targetAddress = scanner.nextLine();
    }

    private static CardChannel openCardChannel(CardTerminal terminal) throws CardException {
        Card card = terminal.connect("*");
        CardChannel channel = card.getBasicChannel();
        return channel;
    }

    private static boolean selectApplet(CardChannel channel) throws CardException {
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, Constants.APPLET_AID);
        ResponseAPDU res = channel.transmit(select);
        return isSuccessful(res);
    }

    private static boolean isSuccessful(ResponseAPDU responseAPDU){
        return responseAPDU.getSW() == 0x9000;
    }
}
