package com.credenceid.sdkapp.models;

import java.util.Hashtable;

/* Custom class which is used to represent a cards information and type. */
public class CardInfo {
	// Holds card name/ATR, APDU table appropriate to card.
	private String mATR;
	private String mName;
	private String[] mAPDUTable;

	public CardInfo(String ATR,
					String cardName,
					String[] apduTable,
					Hashtable hashtable) {

		mATR = ATR;
		mName = cardName;
		mAPDUTable = apduTable;

		hashtable.put(ATR, this);
	}

	@SuppressWarnings("unused")
	public String
	getATR() {
		return mATR;
	}

	public String
	getCardName() {
		return mName;
	}

	public String[]
	getApduTable() {
		return mAPDUTable;
	}
}
