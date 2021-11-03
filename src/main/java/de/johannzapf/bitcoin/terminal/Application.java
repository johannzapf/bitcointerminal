package de.johannzapf.bitcoin.terminal;

import apdu4j.pcsc.TerminalManager;
import apdu4j.pcsc.terminals.LoggingCardTerminal;
import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.objects.UTXO;
import de.johannzapf.bitcoin.terminal.service.AddressService;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Constants;
import org.bitcoinj.core.Base58;

import javax.smartcardio.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.text.DecimalFormat;
import java.util.*;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class Application {

    private static double amount = 0.005;
    private static String targetAddress = "mseEAdcDqJTry4AwYnbgV8s8rmDZnmY4TX";

    private static Scanner scanner = new Scanner(System.in);
    private static DecimalFormat format = new DecimalFormat("#0.00");

    public static void main(String[] args) throws CardException, NoSuchAlgorithmException {
        System.out.println("-------- Initializing Payment Terminal --------");
        CardTerminal terminal = initializeTerminal();
        System.out.println("Terminal connected: " + terminal.getName());
        //readPaymentParams();

        System.out.println("-------------- Connecting to Card --------------");
        Card card;
        CardChannel channel;
        long start;
        do{
            System.out.println("Waiting for Card...");
            if (!terminal.waitForCardPresent(CARD_READER_TIMEOUT)) {
                System.out.println("ERROR: Terminal timeout");
                return;
            }

            start = System.nanoTime();

            card = terminal.connect("*");
            channel = card.getBasicChannel();

            if (!selectApplet(channel)) {
                System.out.println("ERROR: No Bitcoin Wallet Applet on this Card");
                return;
            }
            System.out.println("Card successfully connected.");

            if (!version(channel)) {
                System.out.println("This card is not compatible with a signature-only terminal");
                terminal.waitForCardAbsent(CARD_READER_TIMEOUT);
                continue;
            }

            if (!connectionMode(channel) && amount > CONTACTLESS_LIMIT) {
                System.out.println("Please insert your Card for amounts greater than " + CONTACTLESS_LIMIT + " BTC");
                terminal.waitForCardAbsent(CARD_READER_TIMEOUT);
            } else {
                break;
            }
        } while (true);

        if(!cardStatus(channel)){
            System.out.println("Bitcoin Wallet on this card is not initialized. Please define a PIN to initialize it:");
            String pin = scanner.nextLine();
            initializeWallet(channel, Integer.parseInt(pin));
        }


        System.out.println("------------ Payment Process Start ------------");
        String btcAddress = getAddress(channel);
        System.out.println(">> Address: " + btcAddress);
        byte[] pubKey = getPubKey(channel);
        System.out.println(">> Public Key: " + bytesToHex(pubKey));

        int sAmount = BTCToSatoshi(amount);

        Address address = AddressService.getAddressInfo(btcAddress);
        System.out.println("Available Balance in this Wallet: " + address.getFinalBalance() +
                " BTC (confirmed: " + address.getConfirmedBalance() + " BTC)");

        List<UTXO> utxos = address.findProperUTXOs(sAmount);

        if(BTCToSatoshi(address.getFinalBalance()) < calculateFee(utxos.size()) + sAmount) {
            System.out.println("ERROR: The funds in this wallet are not sufficient for this transaction.");
            return;
        }

        System.out.println("Transaction requires " + utxos.size() + " input(s)");

        Transaction tx = new Transaction(utxos, sAmount, targetAddress, address.getAddress());
        List<byte[]> signatures = new ArrayList<>(utxos.size());


        System.out.println("Sending to Smartcard for approval...");
        for(byte[] toSign : tx.toSign()){
            signatures.add(signTransaction(channel, toSign));
        }

        double elapsed = ((double)(System.nanoTime()-start))/1_000_000_000;
        System.out.println("You can remove your card (" + format.format(elapsed) + " Seconds)");

        String finalTransaction = TransactionService.createTransaction(tx, signatures, pubKey);
        System.out.println("FINAL TRANSACTION: " + finalTransaction);


        if(!AUTO_BROADCAST){
            System.out.println("Broadcast Transaction to P2P Network (y/n)?");
            if(!scanner.nextLine().equals("y")){
                return;
            }
        }
        String hash = TransactionService.broadcastTransaction(finalTransaction);
        System.out.println("Transaction with hash \"" + hash + "\" was successfully broadcast.");
    }

    private static byte[] signTransaction(CardChannel channel, byte[] transaction) throws CardException{
        CommandAPDU sign = new CommandAPDU(CLA, INS_SIGN, 0x00, 0x00, transaction);
        ResponseAPDU res = channel.transmit(sign);
        if(isSuccessful(res)){
            byte[] data = res.getData();
            return data;
        } else {
            throw new PaymentFailedException("Transaction returned " + Arrays.toString(res.getData()));
        }
    }

    private static boolean version(CardChannel channel) throws CardException {
        CommandAPDU version = new CommandAPDU(CLA, INS_VERSION, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(version);
        if(isSuccessful(res)){
            String ver = hexToAscii(res.getData());
            System.out.println(">> Version: " + ver);
            return ver.charAt(4) == 'S';
        } else {
            throw new PaymentFailedException("Card Version returned " + Arrays.toString(res.getData()));
        }
    }

    private static boolean connectionMode(CardChannel channel) throws CardException {
        CommandAPDU conn = new CommandAPDU(CLA, INS_CONN_MODE, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(conn);
        if(isSuccessful(res)){
            if(res.getData()[0] == (byte) 0){
                System.out.println("Card is connected physically");
                return true;
            } else {
                System.out.println("Card is connected via NFC");
                return false;
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

    private static void initializeWallet(CardChannel channel, int pin) throws CardException {
        CommandAPDU init = new CommandAPDU(CLA, INS_INIT, TESTNET ? P1_TESTNET : P1_MAINNET, 0x00,
                ByteBuffer.allocate(4).putInt(pin).array());
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
            throw new PaymentFailedException("Error getting public key");
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

    private static boolean selectApplet(CardChannel channel) throws CardException {
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, Constants.APPLET_AID);
        ResponseAPDU res = channel.transmit(select);
        return isSuccessful(res);
    }

    private static boolean isSuccessful(ResponseAPDU responseAPDU){
        return responseAPDU.getSW() == 0x9000;
    }
}
