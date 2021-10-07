package de.johannzapf.bitcoin.terminal;

import apdu4j.pcsc.TerminalManager;
import apdu4j.pcsc.terminals.LoggingCardTerminal;
import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.SigningMessageTemplate;
import de.johannzapf.bitcoin.terminal.service.AddressService;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Constants;
import org.bitcoinj.core.Base58;

import javax.smartcardio.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Scanner;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class Application {

    private static double amount = 0.013;
    private static String targetAddress = "mv4rnyY3Su5gjcDNzbMLKBQkBicCtHUtFB";

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws CardException, NoSuchAlgorithmException, PaymentFailedException, InvalidKeySpecException, InvalidKeyException, SignatureException {
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

        CardChannel channel = openCardChannel(terminal);
        if(!selectApplet(channel)){
            System.out.println("ERROR: No Bitcoin Wallet Applet on this Card");
            return;
        }
        System.out.println("Card successfully connected.");

        helloWorld(channel);
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
        //byte[] privKey = getPrivKey(channel);
        //System.out.println(">> Private Key: " + bytesToHex(privKey));

        Address address = AddressService.getAddressInfo(btcAddress);
        System.out.println("Available Balance in this Wallet: " + address.getConfirmedBalance() +
                " BTC (unconfirmed: " + address.getUnconfirmedBalance() + " BTC)");
        if(address.getUnconfirmedBalance() + address.getConfirmedBalance() < amount) {
            System.out.println("ERROR: The funds in this wallet are not sufficient for this transaction.");
            return;
        }
        System.out.println("Creating Transaction...");
        SigningMessageTemplate smt = TransactionService.createSigningMessageTemplate(address, targetAddress, amount);

        System.out.println("Sending to Smartcard for approval...");
        byte[] signature = sendTransaction(channel, smt.doubleHash());
        String finalTransaction = TransactionService.createTransaction(smt, signature, pubKey);
        System.out.println("FINAL TRANSACTION: " + finalTransaction);
    }


    private static byte[] sendTransaction(CardChannel channel, byte[] transaction) throws CardException, PaymentFailedException {
        CommandAPDU pay = new CommandAPDU(CLA, INS_PAY, 0x00, 0x00, transaction);
        ResponseAPDU res = channel.transmit(pay);
        if(isSuccessful(res)){
            byte[] data = res.getData();
            return data;
        } else {
            throw new PaymentFailedException("Transaction returned " + Arrays.toString(res.getData()));
        }

    }

    private static void helloWorld(CardChannel channel) throws CardException, PaymentFailedException {
        CommandAPDU hello = new CommandAPDU(CLA, INS_HELLO, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(hello);
        if(isSuccessful(res)){
            System.out.println(">> " + hexToAscii(res.getData()));
        } else {
            throw new PaymentFailedException("Card Hello returned " + Arrays.toString(res.getData()));
        }
    }

    private static void connectionMode(CardChannel channel) throws CardException, PaymentFailedException {
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

    private static boolean cardStatus(CardChannel channel) throws CardException, PaymentFailedException {
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

    private static void initializeWallet(CardChannel channel) throws CardException, PaymentFailedException {
        CommandAPDU init = new CommandAPDU(CLA, INS_INIT, P1_TESTNET, 0x00);
        if(!isSuccessful(channel.transmit(init))){
            throw new PaymentFailedException("Error initializing Wallet");
        }
    }

    private static String getAddress(CardChannel channel) throws CardException, PaymentFailedException {
        CommandAPDU addr = new CommandAPDU(CLA, INS_GET_ADDR, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(addr);
        if(isSuccessful(res)){
            return Base58.encode(res.getData());
        } else {
            throw new PaymentFailedException("Error getting address");
        }
    }

    private static byte[] getPubKey(CardChannel channel) throws CardException, PaymentFailedException {
        CommandAPDU pubKey = new CommandAPDU(CLA, INS_GET_PUBKEY, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(pubKey);
        if(isSuccessful(res)){
            return res.getData();
        } else {
            throw new PaymentFailedException("Error getting address");
        }
    }

    private static byte[] getPrivKey(CardChannel channel) throws CardException, PaymentFailedException {
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
        return lct;
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
