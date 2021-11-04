# bitcointerminal
Terminal application for [Java Card Bitcoin Wallet](https://github.com/johannzapf/JCBitcoinWallet)

## How to use
1. You need to have Maven installed
1. To set your preferred settings, simply edit the constants in the Settings.java class
1. Run Application.main()

## Used dependencies (via Maven)
* apdu4j (for communication with the smartcard)
* slf4j and lombok (for convenience)
* org.json (to parse BlockCypher API)
* org.bitcoinj (contains some useful classes like Base58)


