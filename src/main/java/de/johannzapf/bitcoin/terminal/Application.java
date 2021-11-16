package de.johannzapf.bitcoin.terminal;

import apdu4j.pcsc.TerminalManager;
import apdu4j.pcsc.terminals.LoggingCardTerminal;
import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.objects.Address;
import de.johannzapf.bitcoin.terminal.objects.UTXO;
import de.johannzapf.bitcoin.terminal.service.AddressService;
import de.johannzapf.bitcoin.terminal.service.PINService;
import de.johannzapf.bitcoin.terminal.service.TransactionService;
import de.johannzapf.bitcoin.terminal.util.Constants;
import de.johannzapf.bitcoin.terminal.util.Util;
import org.bitcoinj.core.Base58;

import javax.smartcardio.*;
import java.util.*;

import static de.johannzapf.bitcoin.terminal.util.Constants.*;
import static de.johannzapf.bitcoin.terminal.Settings.*;
import static de.johannzapf.bitcoin.terminal.util.Util.*;

public class Application {

    private static final Scanner scanner = new Scanner(System.in);


    // You can set PROMPT_FOR_PAYMENT_PARAMS to false and simply edit these variables
    private static double amount = 0.004;
    private static String targetAddress = "mx8hFo32gKFsbSCixfksbCNUhuDGWHzFC3";


    /**
     * The main method. Run this method each time you want to process a new transaction.
     * @param args
     * @throws CardException
     */
    public static void main(String[] args) throws CardException {
        System.out.println("-------- Initializing Payment Terminal --------");
        CardTerminal terminal = initializeTerminal();
        System.out.println("Terminal connected: " + terminal.getName());
        if(PROMPT_FOR_PAYMENT_PARAMS) {
            readPaymentParams();
        }

        System.out.println("-------------- Connecting to Card --------------");
        byte[] transaction;
        do{
            System.out.println("Waiting for Card...");
            if (!terminal.waitForCardPresent(CARD_READER_TIMEOUT)) {
                System.out.println("ERROR: Terminal timeout");
                return;
            }

            Card card = terminal.connect("*");
            PINService.parseControlCodes(card);
            CardChannel channel = card.getBasicChannel();

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

            boolean connectionMode = connectionMode(channel);

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

            String btcAddress = getAddress(channel);
            System.out.println(">> Address: " + btcAddress);

            long sAmount = BTCToSatoshi(amount);

            Address address = AddressService.getAddressInfo(btcAddress);
            System.out.println("Available balance in this wallet: " + address.getFinalBalance() +
                    " BTC (confirmed: " + address.getConfirmedBalance() + " BTC)");

            List<UTXO> utxos = address.findProperUTXOs(sAmount);

            if(BTCToSatoshi(address.getFinalBalance()) < calculateFee(utxos.size()) + sAmount) {
                System.out.println("ERROR: The funds in this wallet are not sufficient for this transaction.");
                return;
            }
            if(utxos.size() > 7){
                throw new PaymentFailedException("No support for Transactions with more than seven inputs");
            }
            System.out.println("Transaction requires " + utxos.size() + " input(s)");

            byte[] txParams = TransactionService.constructTxParams(targetAddress, sAmount, utxos);

            System.out.println("Sending to Smartcard for approval...");
            ResponseAPDU res = createTransaction(channel, txParams);

            if(isSuccessful(res)){
                transaction = res.getData();
            } else if(res.getSW() == SW_NFC_LIMIT_EXCEEDED){
                System.out.println("Please insert your Card for amounts greater than 0.002 BTC");
                terminal.waitForCardAbsent(CARD_READER_TIMEOUT);
                continue;
            } else {
                throw new PaymentFailedException("Transaction returned " + Integer.toHexString(res.getSW()));
            }

            System.out.println("You can remove your card");
            break;
        } while (true);


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

    /**
     * Sends the transaction parameters to the card and returns the resulting ResponseAPDU.
     * @param channel
     * @param txParams
     * @return
     * @throws CardException
     */
    private static ResponseAPDU createTransaction(CardChannel channel, byte[] txParams) throws CardException{
        CommandAPDU pay = new CommandAPDU(CLA, INS_CREATE_TRANSACTION, 0x00, 0x00, txParams);
        return channel.transmit(pay);
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
            return ver.charAt(4) == 'T';
        } else {
            throw new PaymentFailedException("Card Version returned " + Integer.toHexString(res.getSW()));
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
