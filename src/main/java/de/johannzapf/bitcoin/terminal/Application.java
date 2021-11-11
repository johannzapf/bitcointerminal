package de.johannzapf.bitcoin.terminal;

import apdu4j.pcsc.TerminalManager;
import apdu4j.pcsc.terminals.LoggingCardTerminal;
import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.Transaction;
import de.johannzapf.bitcoin.terminal.objects.UTXO;
import de.johannzapf.bitcoin.terminal.service.AddressService;
import de.johannzapf.bitcoin.terminal.service.PINService;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Constants;
import org.bitcoinj.core.Base58;

import javax.smartcardio.*;
import java.security.*;
import java.text.DecimalFormat;
import java.util.*;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;
import static de.johannzapf.bitcoin.terminal.Settings.*;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class Application {

    private static final Scanner scanner = new Scanner(System.in);
    private static final DecimalFormat format = new DecimalFormat("#0.00");


    // You can set PROMPT_FOR_PAYMENT_PARAMS to false and simply edit these variables
    private static double amount = 0.0028;
    private static String targetAddress = "mx8hFo32gKFsbSCixfksbCNUhuDGWHzFC3";


    /**
     * The main method. Run this method each time you want to process a new transaction.
     * @param args
     * @throws CardException
     * @throws NoSuchAlgorithmException
     */
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
        boolean connectionMode;
        do{
            System.out.println("Waiting for Card...");
            if (!terminal.waitForCardPresent(CARD_READER_TIMEOUT)) {
                System.out.println("ERROR: Terminal timeout");
                return;
            }

            card = terminal.connect("*");
            PINService.parseControlCodes(card);
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

            connectionMode = connectionMode(channel);

            if (!connectionMode && amount > CONTACTLESS_LIMIT) {
                System.out.println("Please insert your Card for amounts greater than " + CONTACTLESS_LIMIT + " BTC");
                terminal.waitForCardAbsent(CARD_READER_TIMEOUT);
                continue;
            }

            if(!cardStatus(channel)){
                if(!connectionMode){
                    System.out.println("Please insert the card into the card slot in order to initialize it.");
                    terminal.waitForCardAbsent(CARD_READER_TIMEOUT);
                    continue;
                }
                System.out.println("Please set a PIN on the smart card reader");
                PINService.modifyPin(card);
                System.out.println("Initializing Bitcoin Wallet on this card...");
                initializeWallet(channel);
            }

            break;

        } while (true);

        if(connectionMode){
            System.out.println("-------------- PIN Verification --------------");
            int tries = remainingPINTries(channel);
            if(tries == 0){
                System.out.println("Your card has been locked. Please contact the manufacturer.");
                return;
            }
            System.out.println("Please enter your PIN (" + tries + " tries left)");
            while(true){ // Loop runs until the correct PIN has been entered
                byte[] res = PINService.verifyPin(card);
                if(!bytesToHex(res).equals("9000")){
                    // PIN was not entered correctly
                    tries = remainingPINTries(channel);
                    if(tries == 0){
                        System.out.println("Your card has been locked. Please contact the manufacturer.");
                        return;
                    } else {
                        System.out.println("Wrong PIN, please try again (" + tries + " tries left)");
                    }
                } else {
                    break;
                }
            }
        }


        System.out.println("------------ Payment Process Start ------------");
        long start = System.nanoTime();
        String btcAddress = getAddress(channel);
        System.out.println(">> Address: " + btcAddress);
        byte[] pubKey = getPubKey(channel);
        System.out.println(">> Public Key: " + bytesToHex(pubKey));

        int sAmount = BTCToSatoshi(amount);

        Address address = AddressService.getAddressInfo(btcAddress);
        System.out.println("Available balance in this wallet: " + address.getFinalBalance() +
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

    /**
     * Sends the transaction hash to the card and returns the resulting signature.
     * @param channel
     * @param transaction
     * @return
     * @throws CardException
     */
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

    /**
     * Asks the card which version it has.
     * Returns true if the card is compatible with this type of terminal.
     * @param channel
     * @return
     * @throws CardException
     */
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

    /**
     * Asks the card how it is connected.
     * Returns true if card is connected physically and false if it is connected via NFC.
     * @param channel
     * @return
     * @throws CardException
     */
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

    /**
     * Asks the card for its status.
     * Returns true if the card is initialized (i.e. already has an address).
     * @param channel
     * @return
     * @throws CardException
     */
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

    /**
     * Asks the card how many PIN tries are left.
     * @param channel
     * @return
     * @throws CardException
     */
    private static int remainingPINTries(CardChannel channel) throws CardException {
        CommandAPDU status = new CommandAPDU(CLA, INS_PIN_REMAINING_TRIES, 0x00, 0x00, 0x01);
        ResponseAPDU res = channel.transmit(status);
        if(isSuccessful(res)){
            return res.getData()[0];
        } else {
            throw new PaymentFailedException("Remaining PIN tries returned " + Arrays.toString(res.getData()));
        }
    }

    /**
     * Triggers initializing the card (i.e. creating a keypair and an address).
     * @param channel
     * @throws CardException
     */
    private static void initializeWallet(CardChannel channel) throws CardException {
        CommandAPDU init = new CommandAPDU(CLA, INS_INIT, TESTNET ? P1_TESTNET : P1_MAINNET, 0x00);
        if(!isSuccessful(channel.transmit(init))){
            throw new PaymentFailedException("Error initializing Wallet");
        }
    }

    /**
     * Asks the card for its address.
     * @param channel
     * @return
     * @throws CardException
     */
    private static String getAddress(CardChannel channel) throws CardException {
        CommandAPDU addr = new CommandAPDU(CLA, INS_GET_ADDR, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(addr);
        if(isSuccessful(res)){
            return Base58.encode(res.getData());
        } else {
            throw new PaymentFailedException("Error getting address");
        }
    }

    /**
     * Asks the card for its public key.
     * @param channel
     * @return
     * @throws CardException
     */
    private static byte[] getPubKey(CardChannel channel) throws CardException {
        CommandAPDU pubKey = new CommandAPDU(CLA, INS_GET_PUBKEY, 0x00, 0x00);
        ResponseAPDU res = channel.transmit(pubKey);
        if(isSuccessful(res)){
            return res.getData();
        } else {
            throw new PaymentFailedException("Error getting public key");
        }
    }


    /**
     * This method connects to a smart card reader.
     * @return
     * @throws CardException
     */
    private static CardTerminal initializeTerminal() throws CardException {
        TerminalManager.fixPlatformPaths();
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        if(terminals.size() == 0){
            throw new PaymentFailedException("No smart card reader found");
        }
        CardTerminal terminal = factory.terminals().list().get(0);
        LoggingCardTerminal lct = LoggingCardTerminal.getInstance(terminal);
        return DEBUG ? lct : terminal;
    }

    /**
     * Helper method to read parameters for payment (amount and recipient).
     */
    private static void readPaymentParams(){
        System.out.println("Payment Amount (BTC): ");
        amount = Double.parseDouble(scanner.nextLine());
        System.out.println("Target Bitcoin Address: ");
        targetAddress = scanner.nextLine();
    }

    /**
     * Tells the JCVM to select our wallet applet.
     * @param channel
     * @return
     * @throws CardException
     */
    private static boolean selectApplet(CardChannel channel) throws CardException {
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, Constants.APPLET_AID);
        ResponseAPDU res = channel.transmit(select);
        return isSuccessful(res);
    }

    /**
     * Returns true if the Command-APDU was executed successfully.
     * @param responseAPDU
     * @return
     */
    private static boolean isSuccessful(ResponseAPDU responseAPDU){
        return responseAPDU.getSW() == 0x9000;
    }
}
