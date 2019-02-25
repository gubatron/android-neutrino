package cash.bchd.android_neutrino.wallet;

import android.content.Context;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import walletrpc.Api;
import walletrpc.WalletLoaderServiceGrpc;
import walletrpc.WalletServiceGrpc;

/**
 * Wallet represents an instance of a bchwallet. It will load and start the
 * bchwallet daemon and provide convience methods to the wallet's API calls.
 */
public class Wallet {

    private String getConfigFilePath;
    private final String host = "127.0.0.1";
    private final int port = 8332;
    private ManagedChannel channel;
    public static final io.grpc.Context.Key<String> AUTH_TOKEN_KEY = io.grpc.Context.key("AuthenticationToken");
    private AuthCredentials creds;

    private final String DEFAULT_PASSPHRASE = "LETMEIN";

    private final String MAINNET_URI_PREFIX = "bitcoincash:";

    private HashMap<String, AddressListener> addressListeners = new HashMap<String, AddressListener>();

    private int lastBlockHeight;

    private boolean running;

    /**
     * The wallet constructor takes in a context which it uses to derive the config file
     * path and appdatadir.
     */
    public Wallet(Context context, Config config) {
        this.getConfigFilePath = config.getConfigFilePath();
        this.creds = new AuthCredentials(config.getAuthToken());
        this.channel = ManagedChannelBuilder.forAddress(this.host, this.port).usePlaintext().build();
        try {
            config.save(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadWallet(WalletEventListener listener) throws Exception {
        if (!walletExists()) {
            String mnemonic = generateMnemonic();
            WalletLoaderServiceGrpc.WalletLoaderServiceFutureStub stub = WalletLoaderServiceGrpc.newFutureStub(this.channel).withCallCredentials(this.creds);
            ByteString pw = ByteString.copyFromUtf8(DEFAULT_PASSPHRASE);

            Api.CreateWalletRequest request = Api.CreateWalletRequest.newBuilder().setPrivatePassphrase(pw).setMnemonicSeed(mnemonic).build();
            stub.createWallet(request);
            listener.onWalletCreated(mnemonic);
        }

        Thread thread = new Thread(){
            public void run(){
                while (true) {
                    try {
                        WalletServiceGrpc.WalletServiceBlockingStub stub = WalletServiceGrpc.newBlockingStub(channel).withCallCredentials(creds);
                        Api.PingRequest request = Api.PingRequest.newBuilder().build();
                        stub.ping(request);
                        listener.onWalletReady();
                        listener.onBalanceChange(balance());
                        getTransactions(listener);
                        listenTransactions(listener);
                        running = true;
                        break;
                    } catch (Exception e) {}
                }
            }
        };
        thread.start();
    }

    public boolean isRunning() {
        return running;
    }

    public String uriPrefix() {
        return MAINNET_URI_PREFIX;
    }

    public void listenTransactions(WalletEventListener listener) {
        try {
            this.lastBlockHeight = network().getBestHeight();
            listener.onBlock(this.lastBlockHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }

        WalletServiceGrpc.WalletServiceStub stub = WalletServiceGrpc.newStub(channel).withCallCredentials(creds);
        Api.TransactionNotificationsRequest request = Api.TransactionNotificationsRequest.newBuilder().build();
        stub.transactionNotifications(request, new StreamObserver<Api.TransactionNotificationsResponse>() {
            @Override
            public void onNext(Api.TransactionNotificationsResponse value) {
                List<Api.BlockDetails> blocks = value.getAttachedBlocksList();
                if (value.getDetachedBlocksCount() > 0) {
                    lastBlockHeight = 0;
                }
                boolean hasMinedTransactions = false;
                for (Api.BlockDetails block : blocks) {
                    if (block.getHeight() > lastBlockHeight) {
                        lastBlockHeight = block.getHeight();
                        listener.onBlock(block.getHeight());
                    }
                    if (block.getTransactionsCount() > 0) {
                        hasMinedTransactions = true;
                        for (Api.TransactionDetails tx : block.getTransactionsList()) {
                            for (Api.TransactionDetails.Output output : tx.getCreditsList()) {
                                Object obj = addressListeners.get(output.getAddress());
                                if (obj != null) {
                                    AddressListener addrListener = (AddressListener) obj;
                                    addrListener.onPaymentReceived();
                                }
                            }
                        }
                    }
                }

                if (value.getUnminedTransactionsCount() > 0 || hasMinedTransactions) {
                    for (Api.TransactionDetails tx : value.getUnminedTransactionsList()) {
                        for (Api.TransactionDetails.Output output : tx.getCreditsList()) {
                            AddressListener addrListener = (AddressListener) addressListeners.get(output.getAddress());
                            if (addrListener != null) {
                                addrListener.onPaymentReceived();
                            }
                        }
                    }
                    try {
                        long bal = balance();
                        listener.onBalanceChange(bal);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {

            }
        });
    }

    public void listenAddress(String address, AddressListener listener) {
        addressListeners.put(address, listener);
    }

    public String currentAddress() throws Exception {
        WalletServiceGrpc.WalletServiceBlockingStub stub = WalletServiceGrpc.newBlockingStub(this.channel).withCallCredentials(this.creds);
        Api.CurrentAddressRequest request = Api.CurrentAddressRequest.newBuilder().build();
        Api.CurrentAddressResponse reply = stub.currentAddress(request);
        return reply.getAddress();
    }

    public long balance() throws Exception {
        WalletServiceGrpc.WalletServiceBlockingStub stub = WalletServiceGrpc.newBlockingStub(this.channel).withCallCredentials(this.creds);
        Api.BalanceRequest request = Api.BalanceRequest.newBuilder().setAccountNumber(0).setRequiredConfirmations(0).build();
        Api.BalanceResponse reply = stub.balance(request);
        return reply.getSpendable();
    }

    public Api.NetworkResponse network() throws Exception {
        WalletServiceGrpc.WalletServiceBlockingStub stub = WalletServiceGrpc.newBlockingStub(channel).withCallCredentials(creds);
        Api.NetworkRequest request = Api.NetworkRequest.newBuilder().build();
        Api.NetworkResponse reply = stub.network(request);
        return reply;
    }

    private boolean walletExists() throws Exception {
        WalletLoaderServiceGrpc.WalletLoaderServiceBlockingStub stub = WalletLoaderServiceGrpc.newBlockingStub(this.channel).withCallCredentials(this.creds);
        Api.WalletExistsRequest request = Api.WalletExistsRequest.newBuilder().build();
        Api.WalletExistsResponse reply = stub.walletExists(request);
        return reply.getExists();
    }

    private String generateMnemonic() throws Exception {
        WalletLoaderServiceGrpc.WalletLoaderServiceBlockingStub stub = WalletLoaderServiceGrpc.newBlockingStub(this.channel).withCallCredentials(this.creds);
        Api.GenerateMnemonicSeedRequest request = Api.GenerateMnemonicSeedRequest.newBuilder().setBitSize(128).build();
        Api.GenerateMnemonicSeedResponse reply = stub.generateMnemonicSeed(request);
        return reply.getMnemonic();
    }

    private void getTransactions(WalletEventListener listener) throws Exception {
        WalletServiceGrpc.WalletServiceFutureStub stub = WalletServiceGrpc.newFutureStub(this.channel).withCallCredentials(this.creds);
        Api.GetTransactionsRequest request = Api.GetTransactionsRequest.newBuilder().build();
        ListenableFuture<Api.GetTransactionsResponse> reply = stub.getTransactions(request);

        Futures.addCallback(reply, new FutureCallback<Api.GetTransactionsResponse>() {
            @Override
            public void onSuccess(Api.GetTransactionsResponse result) {
                int bestHeight = 0;
                try {
                    bestHeight = network().getBestHeight();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                List<TransactionData> txs = new ArrayList<TransactionData>();
                for (Api.BlockDetails block : result.getMinedTransactionsList()) {
                    for (Api.TransactionDetails tx : block.getTransactionsList()) {
                        txs.add(extractTransactionData(tx, block.getHeight()));
                    }
                }
                for (Api.TransactionDetails tx : result.getUnminedTransactionsList()) {
                    txs.add(extractTransactionData(tx, 0));
                }
                listener.onGetTransactions(Lists.reverse(txs), bestHeight);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private TransactionData extractTransactionData(Api.TransactionDetails tx, int height) {
        long total = 0;
        String toAddress = "";
        for (Api.TransactionDetails.Output output : tx.getCreditsList()) {
            total += output.getAmount();
            toAddress = output.getAddress();
        }
        for (Api.TransactionDetails.Input input : tx.getDebitsList()) {
            total -= input.getPreviousAmount();
        }
        if (total > 0) {
            toAddress = "";
        }
        byte[] idBytes = new byte[32];
        tx.getHash().copyTo(idBytes, 0);

        String txid = BaseEncoding.base16().lowerCase().encode(idBytes);
        TransactionData txData = new TransactionData(txid, (total > 0), "", total, "", "", tx.getTimestamp(), toAddress, height);
        return txData;
    }

    /**
     * Start will start the bchwallet daemon with the requested config options
     */
    public void start() {
        mobile.Mobile.startWallet(this.getConfigFilePath);
    }

    /**
     * Stop cleanly shuts down the wallet daemon. This must be called on close to guarantee
     * no data is corrupted.
     */
    public void stop() {
        channel.shutdown();
        mobile.Mobile.stopWallet();
    }
}
