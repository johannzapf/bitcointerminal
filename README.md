# bitcointerminal
Terminal application for [Java Card Bitcoin Wallet](https://github.com/johannzapf/JCBitcoinWallet)

## How to use
1. Open this project in an IDE of your choice (which needs to support Maven)
1. To set your preferred settings, simply edit the constants in the Settings.java class
1. Connect a smart card reader to your computer
1. Run Application.main() for each payment process you want to initiate

## Used dependencies (via Maven)
* apdu4j (for communication with the smart card)
* slf4j and lombok (for convenience)
* org.json (to parse BlockCypher API)
* org.bitcoinj (contains some useful classes like Base58)


