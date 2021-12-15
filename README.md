# bitcointerminal
Terminal application for [Java Card Bitcoin Wallet](https://github.com/johannzapf/JCBitcoinWallet)

## How to use
1. Open this project in an IDE of your choice (which needs to support Maven)
1. To set your preferred settings, simply edit the constants in the Settings.java class
1. Connect a smart card reader to your computer
1. Run Application.main() for each payment process you want to initiate

## Versions
There are two incompatible versions of this application:
1. Version A
    * can be found on the *signaturecard*-Branch
    * is only compatible with the *signaturecard*-Branch of the applet
    * smart card is only used to sign transactions prepared by the terminal

2. Version B (this version)
    * can be found on the *master*-Branch
    * is only compatible with the *master*-Branch of the applet
    * smart card creates the transaction on its own with data sent from the terminal

## Used dependencies (via Maven)
* apdu4j (for communication with the smart card)
* slf4j and lombok (for convenience)
* org.json (to parse BlockCypher API)
* org.bitcoinj (contains some useful classes like Base58)


