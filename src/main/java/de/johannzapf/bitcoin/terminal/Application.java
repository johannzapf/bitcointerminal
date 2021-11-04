package de.johannzapf.bitcoin.terminal;

import apdu4j.pcsc.SCard;
import apdu4j.pcsc.TerminalManager;
import apdu4j.pcsc.terminals.LoggingCardTerminal;
import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.UTXO;
import de.johannzapf.bitcoin.terminal.service.AddressService;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import org.bitcoinj.core.Base58;

import javax.smartcardio.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.text.DecimalFormat;
import java.util.*;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;
import static de.johannzapf.bitcoin.terminal.Settings.*;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class Application {

    private static double amount = 0.0277;
    private static String targetAddress = "mx8hFo32gKFsbSCixfksbCNUhuDGWHzFC3";

    private static Scanner scanner = new Scanner(System.in);
    private static DecimalFormat format = new DecimalFormat("#0.00");


    public static void main(String[] args) throws CardException, NoSuchAlgorithmException {
        System.out.println("-------- Initializing Payment Terminal --------");
        CardTerminal terminal = initializeTerminal();
        System.out.println("Terminal connected: " + terminal.getName());
        if(PROMPT_FOR_PAYMENT_PARAMS) {
            readPaymentParams();
        }

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
                System.out.println("This card is only compatible with a signature-only terminal");
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
            String newPin = "1234";//scanner.nextLine();
            initializeWallet(channel, Integer.parseInt(newPin));
        }

/*
        System.out.println("-------------- PIN Verification --------------");

        int CM_IOCTL_GET_FEATURE_REQUEST = SCard.CARD_CTL_CODE(3400);
        byte[] resp1 = card.transmitControlCommand(CM_IOCTL_GET_FEATURE_REQUEST, new byte[0]);


        byte[] command = new byte[]{
                0x00,                   // timeout
                0x00,                   // timeout
                (byte) 0x82,                   // format
                0x04,                   // PIN length format
                0x00,
                0x04,                   // Min pin size
                0x04,                   // Max pin size
                0x02,                   // Entry validation condition
                0x01,                   // Number of messages to display
                0x04, 0x09,             // English
                0x00,                   // Message "Enter pin"
                0x00, 0x00, 0x00,       // Non significant here
                0x0D, 0x00, 0x00, 0x00, // Length of the apdu once formatted
                (byte) CLA, INS_VERIFY_PIN, 0x00, 0x00, // APDU command VERIFY
                0x08,                   // APDU command Data length
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF // APDU command PIN + filler
        };

        int CM_IOCTL_VERIFY_PIN_REQUEST = 0x42000DB2;//SCard.CARD_CTL_CODE(2600);
        byte[] resp2 = card.transmitControlCommand(CM_IOCTL_VERIFY_PIN_REQUEST, command);
*/

        System.out.println("------------ Payment Process Start ------------");
        String btcAddress = getAddress(channel);
        System.out.println(">> Address: " + btcAddress);

        long sAmount = BTCToSatoshi(amount);

        Address address = AddressService.getAddressInfo(btcAddress);
        System.out.println("Available Balance in this Wallet: " + address.getFinalBalance() +
                " BTC (confirmed: " + address.getConfirmedBalance() + " BTC)");

        List<UTXO> utxos = address.findProperUTXOs(sAmount);

        if(BTCToSatoshi(address.getFinalBalance()) < calculateFee(utxos.size()) + sAmount) {
            System.out.println("ERROR: The funds in this wallet are not sufficient for this transaction.");
            return;
        }
        if(utxos.size() > 3){
            throw new PaymentFailedException("No support for Transactions with more than three inputs");
        }
        System.out.println("Transaction requires " + utxos.size() + " input(s)");

        byte[] txParams = TransactionService.constructTxParams(targetAddress, sAmount, utxos);

        System.out.println("Sending to Smartcard for approval...");
        byte[] transaction = createTransaction(channel, txParams);

        double elapsed = ((double)(System.nanoTime()-start))/1_000_000_000;
        System.out.println("You can remove your card (" + format.format(elapsed) + " Seconds)");

        String finalTransaction = Util.bytesToHex(transaction);
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

    private static byte[] createTransaction(CardChannel channel, byte[] params) throws CardException{
        CommandAPDU pay = new CommandAPDU(CLA, INS_CREATE_TRANSACTION, 0x00, 0x00, params);
        ResponseAPDU res = channel.transmit(pay);
        if(isSuccessful(res)){
            byte[] data = res.getData();
            return data;
        } else {
            throw new PaymentFailedException("Transaction returned " + Integer.toHexString(res.getSW()));
        }
    }

    private static boolean version(CardChannel channel) throws CardException {
        CommandAPDU version = new CommandAPDU(CLA, INS_VERSION, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(version);
        if(isSuccessful(res)){
            String ver = hexToAscii(res.getData());
            System.out.println(">> Version: " + ver);
            return ver.charAt(4) == 'T';
        } else {
            throw new PaymentFailedException("Card Version returned " + Integer.toHexString(res.getSW()));
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

    private static CardTerminal initializeTerminal() throws NoSuchAlgorithmException, CardException {
        TerminalManager.fixPlatformPaths();
        TerminalFactory factory = TerminalFactory.getDefault();
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
