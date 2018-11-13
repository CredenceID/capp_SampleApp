package com.cid.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cid.sdk.util.Hex;
import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.biometrics.CardCommandResponse;

import java.util.Locale;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;

@SuppressWarnings("unused")
public class CardReaderActivity
		extends Activity {

	private static final String TAG = CardReaderActivity.class.getSimpleName();

	private static final int mEMPTY_STRING_LEN = 0;
	private static final int mREAD_SPECIAL_APDU_LEN = 10;

	/* CredenceSDK biometrics object, used to interface with APIs. */
	@SuppressLint("StaticFieldLeak")
	private static BiometricsManager mBiometricsManager;

	/*
	 * Components in layout file.
	 */
	private TextView mCardReaderStatusTextView;
	private TextView mCardStatusTextView;
	private TextView mDataTextView;
	private CheckBox mSyncCheckbox;
	private Button mOpenCloseButton;
	private EditText mWriteDataEditText;
	private Button mWriteToCardButton;
	private Spinner mReadAPDUSelectSpinner;
	private Button mReadFromCardButton;

	/* Keeps track of card reader sensor. If true then sensor is open, if false sensor is closed. */
	private boolean mIsCardReaderOpen = false;
	/* Keeps track of if card is present on sensor. If true card is present, if false no card is
	 * present.
	 */
	private boolean misCardPresent = false;

	/*
	 * Different types of APDUs to read data from MiFare cards.
	 */
	/* Reads 4096 (4K) number of bytes from card. */
	private String mAPDURead4k = "FF"         // MiFare Card
			+ "B0"                            // MiFare Card READ Command
			+ "00"                            // P1
			+ "00"                            // P2: Block Number
			+ "001000";                       // Number of bytes to read
	/* Reads 2048 (2K) number of bytes from card. */
	private String mAPDURead2k = "FF"         // MiFare Card
			+ "B0"                            // MiFare Card READ Command
			+ "00"                            // P1
			+ "00"                            // P2: Block Number
			+ "000800";                       // Number of bytes to read
	/* Reads 1024 (1K) number of bytes from card. */
	private String mAPDURead1k = "FF"         // MiFare Card
			+ "B0"                            // MiFare Card READ Command
			+ "00"                            // P1
			+ "00"                            // P2: Block Number
			+ "000400";                       // Number of bytes to read

	/* This APDU is used to read "mSpecialData" written to the card. */
	private String mAPDUReadSpecialData = "FF"  // MiFare Card
			+ "B0"                              // MiFare Card READ Command
			+ "00"                              // P1
			+ "01"                              // P2: Block Number
			+ "00";                             // Number of bytes to read

	/*
	 * Different types of APDUs to write data to MiFare cards.
	 */
	/* Writes 4096 (4K) number of bytes to card. */
	private String mAPDUWrite4k = "FF"        // MiFare Card
			+ "D6"                            // MiFare Card READ Command
			+ "00"                            // P1
			+ "00"                            // P2: Block Number
			+ "001000";                       // Number of bytes to read
	/* Writes 2048 (2K) number of bytes to card. */
	private String mAPDUWrite2k = "FF"        // MiFare Card
			+ "D6"                            // MiFare Card READ Command
			+ "00"                            // P1
			+ "00"                            // P2: Block Number
			+ "000800";                       // Number of bytes to read
	/* Writes 1024 (1K) number of bytes to card. */
	private String mAPDUWrite1k = "FF"        // MiFare Card
			+ "D6"                            // MiFare Card READ Command
			+ "00"                            // P1
			+ "00"                            // P2: Block Number
			+ "000400";                       // Number of bytes to read

	/* Data to be written to card will be stored here. */
	private byte[] mSpecialData = null;

	/* APDU executed when "mReadFromCardButton" is clicked. This APDU will change each time a
	 * different type of read is selected via "mReadAPDUSelectSpinner".
	 */
	private String mReadAPDUCommand = mAPDURead1k;

	/* Callback invoked each time sensor detects a card change. */
	private Biometrics.OnCardStatusListener onCardStatusListener = (String ATR,
																	int prevState,
																	int currentState) -> {
		/* If currentState is 1, then no card is present. */
		if (currentState == 1) {
			misCardPresent = false;
			mCardStatusTextView.setText(getString(R.string.card_absent));
		} else {
			misCardPresent = true;
			mCardStatusTextView.setText(getString(R.string.card_present));
		}

		/* currentStates [2, 6] represent a card present. If a card is present code will reach.
		 * Here you may perform any operations you want ran automatically when a card is detected.
		 */
	};

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cardreader);

		mBiometricsManager = MainActivity.getBiometricsManager();

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
	}

	/* Invoked when user pressed back menu button. */
	@Override
	public void
	onBackPressed() {
		super.onBackPressed();

		/* If back button is pressed when we want to destroy activity. */
		this.onDestroy();
	}

	/* Invoked when application is killed, either by user or system. */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		/* Close card reader since user is exiting activity. */
		mBiometricsManager.cardCloseCommand();

		/* If user presses back button then they are exiting application. If this is the case then
		 * tell C-Service to unbind from this application.
		 */
		mBiometricsManager.finalizeBiometrics(false);
	}

	/* Initializes all objects inside layout file. */
	private void
	initializeLayoutComponents() {
		mCardReaderStatusTextView = findViewById(R.id.status_textview);
		mCardStatusTextView = findViewById(R.id.cardstatus_textview);
		mDataTextView = findViewById(R.id.data_textview);

		mSyncCheckbox = findViewById(R.id.sync_checkbox);
		mOpenCloseButton = findViewById(R.id.open_close_button);

		mWriteDataEditText = findViewById(R.id.write_edittext);
		mWriteToCardButton = findViewById(R.id.write_data_button);

		mReadAPDUSelectSpinner = findViewById(R.id.read_apdu_selector_spinner);
		mReadFromCardButton = findViewById(R.id.read_data_button);
	}

	/* Configure all objects in layout file, set up listeners, views, etc. */
	private void
	configureLayoutComponents() {
		/* Disable views which allow user to read/write to/from card until card reader is open. */
		this.setReadWriteComponentEnable(false);

		/* This will remove focus from view, meaning keyboard will hide. */
		mWriteDataEditText.clearFocus();

		mOpenCloseButton.setOnClickListener((View v) -> {
			mOpenCloseButton.setEnabled(false);
			mCardStatusTextView.setText("");

			/* Based on status of card reader take appropriate action. */
			if (mIsCardReaderOpen)
				mBiometricsManager.cardCloseCommand();
			else this.openCardReader();
		});

		/* Each time an item is selected we need up update "mReadAPDUCommand". This is so when user
		 * presses "mReadFromCardButton", APDU which matches  selected option has already been set.
		 */
		mReadAPDUSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void
			onItemSelected(AdapterView<?> parent,
						   View view,
						   int position,
						   long id) {

				if (0 == position)
					mReadAPDUCommand = mAPDUReadSpecialData;
				else if (1 == position)
					mReadAPDUCommand = mAPDURead1k;
				else if (2 == position)
					mReadAPDUCommand = mAPDURead2k;
				else if (3 == position)
					mReadAPDUCommand = mAPDURead4k;
			}

			@Override
			public void
			onNothingSelected(AdapterView<?> parent) {
			}
		});

		mWriteToCardButton.setOnClickListener((View v) -> {
			/* Do not do anything if card is not present. */
			if (!misCardPresent) {
				mCardReaderStatusTextView.setText(getString(R.string.no_card_present_to_write_to));
				return;
			}

			/* Check to make sure user has entered some valid data to write to card. If nothing
			 * exists then do not do anything.
			 */
			String data = mWriteDataEditText.getText().toString();
			if (mEMPTY_STRING_LEN == data.length()) {
				mCardReaderStatusTextView.setText(getString(R.string.no_data_to_write_to_card));
				return;
			}

			/* Disable UI components so they do not interfere with ongoing operation. */
			this.setReadWriteComponentEnable(false);

			/* Save data to write inside our global variable. */
			mSpecialData = data.getBytes();

			/* Based on if user has selected sync/async APIs call appropriate method. */
			if (mSyncCheckbox.isChecked())
				writeCardSync(mSpecialData);
			else writeCardAsync(mSpecialData);
		});

		mReadFromCardButton.setOnClickListener((View v) -> {
			/* Do not do anything if card is not present. */
			if (!misCardPresent) {
				mCardReaderStatusTextView.setText(getString(R.string.no_card_present_read_from));
				return;
			}

			/* Disable UI components so they do not interfere with ongoing operation. */
			this.setReadWriteComponentEnable(false);

			/* Based on if user has selected sync/async APIs call appropriate method. */
			if (mSyncCheckbox.isChecked())
				readCardSync(mReadAPDUCommand);
			else readCardAsync(mReadAPDUCommand);
		});
	}

	/* Calls Credence APIs to open card reader. */
	private void
	openCardReader() {
		/* Let user know card reader will now try to be opened. */
		mCardReaderStatusTextView.setText(getString(R.string.opening_card_reader));

		mBiometricsManager.cardOpenCommand(new Biometrics.CardReaderStatusListener() {
			@Override
			public void onCardReaderOpen(Biometrics.ResultCode resultCode) {
				/* Re-able button since sensor has returned back with binary answer yes or no. */
				mOpenCloseButton.setEnabled(true);

				if (FAIL == resultCode) {
					mCardReaderStatusTextView.setText(getString(R.string.failed_open_card_reader));
					return;
				}

				mIsCardReaderOpen = true;

				mCardReaderStatusTextView.setText(getString(R.string.card_reader_opened));
				mOpenCloseButton.setText(getString(R.string.close_card_reader));

				setReadWriteComponentEnable(true);

				/* If card reader opened successfully, register a listener will be invoked each time
				 * card reader's status changes. Meaning that anytime a card is placed/removed
				 * invoke this callback.
				 */
				mBiometricsManager.registerCardStatusListener(onCardStatusListener);
			}

			@SuppressLint("SetTextI18n")
			@Override
			public void onCardReaderClosed(Biometrics.ResultCode resultCode,
										   Biometrics.CloseReasonCode closeReasonCode) {
				mOpenCloseButton.setEnabled(true);

				if (FAIL == resultCode)
					return;

				mIsCardReaderOpen = false;

				mCardReaderStatusTextView.setText("Card reader closed: " + closeReasonCode.name());
				mCardStatusTextView.setText("");
				mOpenCloseButton.setText(getString(R.string.open_card_reader));

				setReadWriteComponentEnable(false);
			}
		});
	}

	/* Set enable for components which allow user to read/write from/to card and form APDUs.
	 *
	 * @param enabled If true enables components, if false dis-ables them.
	 */
	private void
	setReadWriteComponentEnable(boolean enabled) {
		mWriteDataEditText.setEnabled(enabled);
		mWriteToCardButton.setEnabled(enabled);
		mReadAPDUSelectSpinner.setEnabled(enabled);
		mReadFromCardButton.setEnabled(enabled);
	}

	/* This method attempts to read 1K, 2K, and 4K blocks of data off of a card. You pass in either
	 * one of three mAPDURead(4|2|1)k commands. It will execute this command then try the next size
	 * block.
	 *
	 * ie. If you pass 1K command, it will next try the 2K then 4K.
	 * ie. If you pass 2K command, it will next try 4K.
	 * ie. If you pass 4K after command is done, it will do nothing.
	 *
	 * The purpose of this function is to demonstrate how you may execute multiple APDUs linearly
	 * using async. APIs. The core of this is a "state machine".
	 *
	 * @param APDUCommand Initial APDU command to execute.
	 */
	@SuppressWarnings("StringConcatenationInLoop")
	private void
	readCardAsync(String APDUCommand) {
		mCardReaderStatusTextView.setText(getString(R.string.reading_card_wait));

		mBiometricsManager.cardCommand(new ApduCommand(APDUCommand),
				false,
				(Biometrics.ResultCode resultCode, byte sw1, byte sw2, byte[] data) -> {
					this.setReadWriteComponentEnable(true);

					if (resultCode == FAIL) {
						mCardReaderStatusTextView.setText(getString(R.string.done_reading_from_card));
						mDataTextView.setText(getString(R.string.apdu_failed));
						return;
					}

					String dataToDisplay;
					if (mREAD_SPECIAL_APDU_LEN == mReadAPDUCommand.length()) {
						dataToDisplay = "";
						/* Convert read data into human readable ASCII characters. */
						for (byte aData : data)
							dataToDisplay += (char) aData;
					} else dataToDisplay = Hex.toString(data);

					String str = String.format(Locale.ENGLISH,
							"SW1: %s, SW2: %s\nLength of data read: %d\n\n %s",
							Hex.toString(sw1),
							Hex.toString(sw2),
							data.length,
							dataToDisplay);

					mCardReaderStatusTextView.setText(getString(R.string.done_reading_from_card));
					mDataTextView.setText(str);
				});
	}

	/* This method attempts to read 1K, 2K, and 4K blocks of data off of a card. It will attempts to
	 * read each block of data linearly. It will first try 1K, then 2K, then 4K card reads.
	 *
	 * The purpose of this function is to demonstrate how you may execute multiple APDUs linearly
	 * using sync. APIs.
	 */
	@SuppressWarnings("StringConcatenationInLoop")
	private void
	readCardSync(String APDUCommand) {
		mCardReaderStatusTextView.setText(getString(R.string.reading_card_wait));

		new Thread(() -> {
			CardCommandResponse response
					= mBiometricsManager.cardCommandSync(new ApduCommand(APDUCommand), false, 4000);

			/* If APDU failed then response will be NULL. */
			if (response == null) {
				runOnUiThread(() -> {
					mCardReaderStatusTextView.setText(getString(R.string.done_reading_from_card));
					mDataTextView.setText(getString(R.string.apdu_failed));
					this.setReadWriteComponentEnable(true);
				});
				return;
			}

			String dataToDisplay;
			if (mREAD_SPECIAL_APDU_LEN == mReadAPDUCommand.length()) {
				dataToDisplay = "";
				/* Convert read data into human readable ASCII characters. */
				for (int i = 0; i < response.data.length; ++i)
					dataToDisplay += (char) response.data[i];
			} else dataToDisplay = Hex.toString(response.data);

			String str = String.format(Locale.ENGLISH,
					"SW1: %s, SW2: %s\nLength of data read: %d\n\n %s",
					Hex.toString(response.sw1),
					Hex.toString(response.sw2),
					response.data.length,
					dataToDisplay);

			runOnUiThread(() -> {
				mCardReaderStatusTextView.setText(getString(R.string.done_reading_from_card));
				mDataTextView.setText(str);

				this.setReadWriteComponentEnable(true);
			});
		}).start();
	}

	/* This method attempts to write some data to MiFare card.
	 *
	 * data Data to write to card in String format.
	 */
	private void
	writeCardSync(String data) {
		this.writeCardSync(data.getBytes());
	}

	/* This method attempts to write some data to MiFare card.
	 *
	 * data Data to write to card in byte array format.
	 */
	private void
	writeCardSync(byte[] dataToWrite) {
		new Thread(() -> {
			String apdu = createWriteAPDUCommand((byte) 0x01, dataToWrite);
			CardCommandResponse response
					= mBiometricsManager.cardCommandSync(new ApduCommand(apdu), false, 4000);

			/* If APDU failed then response will be NULL. */
			if (null == response) {
				runOnUiThread(() -> {
					mCardReaderStatusTextView.setText(getString(R.string.done_writing_to_card));
					mDataTextView.setText(getString(R.string.apdu_failed));
					this.setReadWriteComponentEnable(true);
				});
				return;
			}

			runOnUiThread(() -> {
				String str = String.format(Locale.ENGLISH,
						"SW1: %s, SW2: %s",
						Hex.toString(response.sw1),
						Hex.toString(response.sw2));

				mCardReaderStatusTextView.setText(getString(R.string.done_writing_to_card));
				mDataTextView.setText(str);

				this.setReadWriteComponentEnable(true);

				/* If a write was successful we should then update "mAPDUReadSpecialData" so that it
				 * will same number of bytes that were written.
				 */
				this.updateReadSpecialAPDU(mSpecialData);
			});
		}).start();
	}

	/* This method attempts to write some data to MiFare card. After writing data it will then try
	 * to read that same data back.
	 */
	private void
	writeCardAsync(byte[] dataToWrite) {
		String apdu = createWriteAPDUCommand((byte) 0x01, dataToWrite);

		mBiometricsManager.cardCommand(new ApduCommand(apdu), false, (ResultCode resultCode,
																	  byte sw1,
																	  byte sw2,
																	  byte[] data) -> {

			if (FAIL == resultCode) {
				mCardReaderStatusTextView.setText(getString(R.string.done_writing_to_card));
				mDataTextView.setText(getString(R.string.apdu_failed));
				this.setReadWriteComponentEnable(true);
				return;
			}

			String str = String.format(Locale.ENGLISH,
					"SW1: %s, SW2: %s",
					Hex.toString(sw1),
					Hex.toString(sw2));

			mCardReaderStatusTextView.setText(getString(R.string.done_writing_to_card));
			mDataTextView.setText(str);

			this.setReadWriteComponentEnable(true);

			/* If a write was successful we should then update "mAPDUReadSpecialData" so that it
			 * will same number of bytes that were written.
			 */
			this.updateReadSpecialAPDU(mSpecialData);
		});
	}

	/* Creates an APDU command for writing data to a MiFare card.
	 *
	 * @param blockNumber	Block number to write data to.
	 * @param data 			Data to write to card.
	 * @return				APDU command in String format.
	 */
	@SuppressWarnings("SameParameterValue")
	private String
	createWriteAPDUCommand(byte blockNumber,
						   byte[] data) {

		final int dataLen = data.length;

		/* 7 MiFare bytes, 2 Data size bytes, CID header bytes+ data */
		byte[] writeAPDU = new byte[7 + dataLen];

		writeAPDU[0] = (byte) 0xFF;                        // MiFare Card Header
		writeAPDU[1] = (byte) 0xD6;                        // MiFare Card WRITE Command
		writeAPDU[2] = (byte) 0x00;                        // P1
		writeAPDU[3] = blockNumber;                        // P2: Block Number
		writeAPDU[4] = (byte) 0x00;                        // Escape Character
		writeAPDU[5] = (byte) ((dataLen >> 8) & 0xFF);     // Number of bytes: MSB
		writeAPDU[6] = (byte) (dataLen & 0xFF);            // Number of bytes: LSB

		/* Append "data" to end of "writeAPDU" byte array. */
		System.arraycopy(data, 0, writeAPDU, 7, dataLen);

		/* Return "writeAPDU" as a String. */
		return Hex.toString(writeAPDU);
	}

	/* After writing "special" data to a card we also want to be able to read it back. This method
	 * will update special APDU read command to read data that was last written to card.
	 *
	 * @param data
	 */
	private void
	updateReadSpecialAPDU(byte[] data) {
		mAPDUReadSpecialData = "FF"   // MiFare Card
				+ "B0"                // MiFare Card READ Command
				+ "00"                // P1
				+ "01";               // P2: Block Number

		mAPDUReadSpecialData += Hex.toString((byte) mSpecialData.length);

		if (mREAD_SPECIAL_APDU_LEN == mReadAPDUCommand.length())
			mReadAPDUCommand = mAPDUReadSpecialData;

	}
}
