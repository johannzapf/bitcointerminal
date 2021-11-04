package de.johannzapf.bitcoin.terminal.util;

import org.bitcoinj.core.NetworkParameters;

import static de.johannzapf.bitcoin.terminal.Settings.TESTNET;

public class Constants {

    public static final byte[] APPLET_AID = new byte[]{1,2,3,4,5,6,7,8,9,0,5};

    public static final int CLA = 0x80;
    public static final int INS_VERSION = 0x00;
    public static final int INS_CONN_MODE = 0x01;
    public static final int INS_STATUS = 0x02;
    public static final int INS_INIT = 0x03;
    public static final int INS_VERIFY_PIN = 0x04;
    public static final int INS_GET_ADDR = 0x05;
    public static final int INS_SIGN = 0x06;
    public static final int INS_GET_PUBKEY = 0x07;

    public static final int P1_MAINNET = 0x01;
    public static final int P1_TESTNET = 0x02;

    public static final NetworkParameters netParams = TESTNET ? NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
            : NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    public static final String BLOCKCYPHER_API = TESTNET ? "https://api.blockcypher.com/v1/btc/test3" :
            "https://api.blockcypher.com/v1/btc/main" ;

}
