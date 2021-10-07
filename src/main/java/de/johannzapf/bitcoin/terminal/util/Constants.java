package de.johannzapf.bitcoin.terminal.util;

import org.bitcoinj.core.NetworkParameters;

public class Constants {

    public static final long CARD_READER_TIMEOUT = 60000;

    public static final byte[] APPLET_AID = new byte[]{1,2,3,4,5,6,7,8,9,0,5};

    public static final int CLA = 0x80;
    public static final int INS_HELLO = 0x00;
    public static final int INS_CONN_MODE = 0x01;
    public static final int INS_STATUS = 0x02;
    public static final int INS_INIT = 0x03;
    public static final int INS_GET_PUBKEY = 0x04;
    public static final int INS_GET_ADDR = 0x05;
    public static final int INS_PAY = 0x06;
    public static final int INS_GET_PRIVKEY = 0x07;

    public static final int P1_MAINNET = 0x01;
    public static final int P1_TESTNET = 0x02;

    public static final NetworkParameters netParams = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);

    public static final String BLOCKCYPHER_API = "https://api.blockcypher.com/v1/btc/test3";

}
