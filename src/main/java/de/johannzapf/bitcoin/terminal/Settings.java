package de.johannzapf.bitcoin.terminal;

public class Settings {

    // When set to true, the whole application will operate in the Bitcoin Testnet environment
    // This includes telling the wallet to generate Testnet addresses
    public static final boolean TESTNET = true;

    // When set to true, the application automatically broadcasts transactions once they are created
    // When set to false, the application asks for confirmation first
    public static final boolean AUTO_BROADCAST = false;

    // When set to true, the application will ask for the recipient and amount at runtime
    // You can set this to false and simply edit the respective instance variables in the application class
    public static final boolean PROMPT_FOR_PAYMENT_PARAMS = true;

    // The minimum number of confirmations a UTXO needs to have in order to be allowed to be spent
    public static final int MINIMUM_CONFIRMATIONS = 0;

    // The fee the application uses, in satoshi per byte
    public static final long FEE = 10;

    // When set to true, the application will log all APDU traffic
    public static final boolean DEBUG = false;

    // The time in milliseconds after which the application will terminate when no card is inserted into the reader
    public static final long CARD_READER_TIMEOUT = 120000;
}
