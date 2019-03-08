package cash.bchd.android_neutrino;

import android.content.SharedPreferences;

public class Settings {

    private static final String INITIALIZED_KEY = "Initialized";
    private static final String MNEMONIC_KEY = "Mnemonic";
    private static final String BLOCKS_ONLY_KEY = "BlocksOnly";
    private static final String LAST_BALANCE_KEY = "LastBalance";
    private static final String FIAT_CURRENCY_KEY = "FiatCurrency";
    private static final String LAST_ADDRESS_KEY = "LastAddress";
    private static final String LAST_BLOCK_HEIGHT_KEY = "LastBlockHeight";
    private static final String LAST_BLOCK_HASH_KEY = "LastBlockHash";
    private static final String FEE_PER_BYTE_KEY = "FeePerByte";
    private static final String DEFAULT_LABEL_KEY = "DefaultLabel";
    private static final String DEFAULT_MEMO_KEY = "DefaultMemo";

    SharedPreferences prefs;

    private static Settings instance;

    Settings(SharedPreferences prefs) {
        this.prefs = prefs;
        instance = this;
    }

    public static Settings getInstance() {
        return instance;
    }

    public void setWalletInitialized(boolean initialized) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putBoolean(INITIALIZED_KEY, initialized);
        editor.apply();
    }

    public Boolean getWalletInitialized() {
        boolean initialized = prefs.getBoolean(INITIALIZED_KEY, false);
        return initialized;
    }

    public void setMnemonic(String mnemonic) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(MNEMONIC_KEY, mnemonic);
        editor.apply();
    }

    public String getMnemonic() {
        String mnemonic = prefs.getString(MNEMONIC_KEY, "");
        return mnemonic;
    }

    public void setBlocksOnly(boolean blocksOnly) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putBoolean(BLOCKS_ONLY_KEY, blocksOnly);
        editor.apply();
    }

    public Boolean getBlocksOnly() {
        boolean blocksOnly = prefs.getBoolean(BLOCKS_ONLY_KEY, false);
        return blocksOnly;
    }

    public void setLastBalance(long balance) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putLong(LAST_BALANCE_KEY, balance);
        editor.apply();
    }

    public long getLastBalance() {
        long balance = prefs.getLong(LAST_BALANCE_KEY, 0);
        return balance;
    }

    public void setFiatCurrency(String currency) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(FIAT_CURRENCY_KEY, currency);
        editor.apply();
    }

    public String getFiatCurrency() {
        String currency = prefs.getString(FIAT_CURRENCY_KEY, "usd");
        return currency;
    }

    public void setLastAddress(String addr) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(LAST_ADDRESS_KEY, addr);
        editor.apply();
    }

    public String getLastAddress() {
        String addr = prefs.getString(LAST_ADDRESS_KEY, "");
        return addr;
    }

    public void setLastBlockHeight(int blockHeight) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putInt(LAST_BLOCK_HEIGHT_KEY, blockHeight);
        editor.apply();
    }

    public int getLastBlockHeight() {
        int height = prefs.getInt(LAST_BLOCK_HEIGHT_KEY, 0);
        return height;
    }

    public void setLastBlockHash(String blockHash) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(LAST_BLOCK_HASH_KEY, blockHash);
        editor.apply();
    }

    public String getLastBlockHash() {
        String hash = prefs.getString(LAST_BLOCK_HASH_KEY, "");
        return hash;
    }

    public void setFeePerByte(int satPerByte) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putInt(FEE_PER_BYTE_KEY, satPerByte);
        editor.apply();
    }

    public int getFeePerByte() {
        int fee = prefs.getInt(FEE_PER_BYTE_KEY, 50);
        return fee;
    }

    public void setDefaultLabel(String label) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(DEFAULT_LABEL_KEY, label);
        editor.apply();
    }

    public String getDefaultLabel() {
        String label = prefs.getString(DEFAULT_LABEL_KEY, "");
        return label;
    }

    public void setDefaultMemo(String memo) {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(DEFAULT_MEMO_KEY, memo);
        editor.apply();
    }

    public String getDefaultMemo() {
        String memo = prefs.getString(DEFAULT_MEMO_KEY, "");
        return memo;
    }
}
