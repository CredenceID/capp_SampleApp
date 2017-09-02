package com.credenceid.sdkapp.models;

import java.util.Hashtable;

/* Custom class which is used to represent different cards. Holds specific card information. */
public class CardInfo {
    /* Holds card name, apdu table, and atr value. */
    private String atr;
    private String cardName;
    private String[] apduTable;

    public CardInfo(String ATR, String cardName, String[] apduTable, Hashtable hashtable) {
        this.atr = ATR;
        this.cardName = cardName;
        this.apduTable = apduTable;

        hashtable.put(atr, this);
    }

    @SuppressWarnings("unused")
    public String getATR() {
        return (atr);
    }

    public String getCardName() {
        return (cardName);
    }

    public String[] getApduTable() {
        return (apduTable);
    }
}
