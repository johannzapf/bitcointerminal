package de.johannzapf.bitcoin.terminal.wallet;

import org.bitcoinj.core.*;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;

public class MyWallet {

    private static String iPhoneWalletAddress = "2Msmnmk2Pu8E9MEwfak7fPbrt4XqXThqAQj";

    public static void main(String[] args) throws UnreadableWalletException, BlockStoreException, IOException, InsufficientMoneyException {

        final NetworkParameters netParams = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
        final File walletFile = new File("mywallet.wallet");

        Wallet wallet = Wallet.loadFromFile(walletFile);

        BlockStore blockStore = new MemoryBlockStore(netParams);
        BlockChain blockChain = new BlockChain(netParams, wallet, blockStore);

        PeerGroup peerGroup = new PeerGroup(netParams, blockChain);
        peerGroup.setUserAgent("Javacard Wallet", "0.1");
        peerGroup.addWallet(wallet);
        peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
        peerGroup.start();
        peerGroup.downloadBlockChain();

        System.out.println("Complete content of the wallet:\n" + wallet);
        wallet.saveToFile(walletFile);

        //sendBTC(netParams, wallet, peerGroup, walletFile);
    }

    private static void sendBTC(NetworkParameters netParams, Wallet wallet, PeerGroup peerGroup, File walletFile) throws InsufficientMoneyException, IOException {
        Address to = Address.fromString(netParams, iPhoneWalletAddress);
        Wallet.SendResult res = wallet.sendCoins(peerGroup, to, Coin.valueOf(0, 1));

        wallet.saveToFile(walletFile);
    }

    private static void createWallet(NetworkParameters netParams, File file) throws IOException {
        Wallet wallet = new Wallet(netParams);
        wallet.importKey(new ECKey());
        wallet.saveToFile(file);
    }
}
