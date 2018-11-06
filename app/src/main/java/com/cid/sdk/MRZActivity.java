package com.cid.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.icao.ICAODocumentData;
import com.credenceid.icao.ICAOReadIntermediateCode;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

@SuppressWarnings("unused")
public class MRZActivity
		extends Activity {

	private final static String TAG = MRZActivity.class.getSimpleName();

	private final int mDATE_OF_BIRTH = 0;
	private final int mDATE_OF_EXPIRY = 1;
	private final int mISSUER = 2;
	private final int mDOCUMENT_TYPE = 3;
	private final int mLAST_NAME = 4;
	private final int mFIRST_NAME = 5;
	private final int mNATIONALITY = 6;
	private final int mPRIMARY_IDENTIFIER = 7;
	private final int mSECONDARY_IDENTIFIER = 8;
	private final int mDOCUMENT_NUMBER = 9;
	private final int mGENDER = 10;

	private TextView mStatusTextView;
	private ImageView mICAOImageView;
	private TextView mICAOTextView;
	private Button mOpenMRZButton;
	private Button mOpenRFButton;

	private BiometricsManager mBiometricsManager;

	private boolean mIsMRZOpen = false;
	private boolean mIsEpassportOpen = false;

	// Listener invoked each time MRZ reader is able to read MRZ text from document.
	private Biometrics.OnMrzReadListener mOnMrzReadListener = (Biometrics.ResultCode resultCode,
															   String hint,
															   byte[] rawData,
															   String data,
															   String parsedData) -> {

		Log.d(TAG, "OnMrzReadListener: Hit: " + hint);
		Log.d(TAG, "OnMrzReadListener: ResultCode: " + resultCode.name());
		Log.d(TAG, "OnMrzReadListener: Data: " + data);
		Log.d(TAG, "OnMrzReadListener: ParsedData: " + parsedData);

		if (resultCode == FAIL)
			mStatusTextView.setText(getString(R.string.mrz_failed_reswipe));
		else if (resultCode == INTERMEDIATE)
			mStatusTextView.setText(getString(R.string.mrz_reading_wait));
		else {
			// Once data is read, C-Service auto parses it and returns it as one big string of data.
			if (parsedData == null || parsedData.isEmpty()) {
				mStatusTextView.setText(getString(R.string.mrz_failed_reswipe));
				return;
			}

			// Each section of data is separated by a "\r\n" character. If we split this data up, we
			// should have TEN elements of data. Please see the constants defined at the top of this
			// class to see the different pieces of information MRZ contains.
			final String[] splitData = parsedData.split("\r\n");
			if (splitData.length < 10) {
				mStatusTextView.setText(getString(R.string.mrz_failed_reswipe));
				return;
			}

			mStatusTextView.setText(getString(R.string.mrz_read_success));
			mICAOTextView.setText(parsedData);

			readICAODocument(splitData[mDATE_OF_BIRTH],
					splitData[mDOCUMENT_NUMBER],
					splitData[mDATE_OF_EXPIRY]);
		}
	};

	// Listener invoked each time C-Service detects a document change from MRZ reader.
	private Biometrics.OnMrzDocumentStatusListener mOnMrzDocumentStatusListener
			= (int previousState, int currentState) -> {

		// If currentState is not 2, then no document is present.
		if (currentState != 2) {
			Log.d(TAG, "OnMrzDocumentStatusListener: No document present.");
			return;
		}

		mStatusTextView.setText(getString(R.string.mrz_reading_wait));

		// If current state is 2, then a document is present on MRZ reader. If a document
		// is present we must read it to obtain MRZ field data. Once we have read MRZ we
		// can then pass along this information to the "readICAODocument()" API.
		//
		// When MRZ is read this callback is invoked "mOnMrzReadListener".
		mBiometricsManager.readMRZ(mOnMrzReadListener);
	};

	// Listener invoked each time C-Service detects a document change from EPassport reader.
	private Biometrics.OnEpassportCardStatusListener mOnEpassportCardStatusListener
			= (int previousState, int currentState) -> {

		// If currentState is not 2, then no document is present.
		if (currentState != 2)
			Log.d(TAG, "Document was removed, no document present.");
	};

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mrz);

		mBiometricsManager = MainActivity.getBiometricsManager();

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
	}

	@Override
	public void
	onBackPressed() {
		super.onBackPressed();

		// If user presses back button then close all open peripherals.
		mBiometricsManager.ePassportCloseCommand();
		mBiometricsManager.closeMRZ();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	@Override
	protected void
	onDestroy() {
		super.onDestroy();

		// If application is being killed then close all open peripherals.
		mBiometricsManager.ePassportCloseCommand();
		mBiometricsManager.closeMRZ();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	private void
	initializeLayoutComponents() {
		mStatusTextView = findViewById(R.id.status_textview);

		mICAOImageView = findViewById(R.id.icao_dg2_imageview);
		mICAOTextView = findViewById(R.id.icao_textview);

		mOpenMRZButton = findViewById(R.id.open_mrz_button);
		mOpenRFButton = findViewById(R.id.open_epassport_buton);
	}

	private void
	configureLayoutComponents() {
		mOpenMRZButton.setEnabled(true);
		mOpenMRZButton.setText(getString(R.string.open_mrz));
		mOpenMRZButton.setOnClickListener((View v) -> {
			if (!mIsMRZOpen)
				openMRZReader();
			else {
				mBiometricsManager.closeMRZ();
				mBiometricsManager.ePassportCloseCommand();
			}
		});

		mOpenRFButton.setEnabled(false);
		mOpenRFButton.setText(getString(R.string.open_epassport));
		mOpenRFButton.setOnClickListener((View v) -> {
			if (!mIsEpassportOpen)
				openEPassportReader();
			else mBiometricsManager.ePassportCloseCommand();
		});
	}

	private void
	openMRZReader() {
		final String localTAG = TAG + ":openMRZReader";
		Log.d(localTAG, "openMRZReader()");

		mStatusTextView.setText(getString(R.string.mrz_opening));

		// Register a listener that will be invoked each time MRZ reader's status changes. Meaning
		// that anytime a document is placed/removed invoke this callback.
		mBiometricsManager.registerMrzDocumentStatusListener(mOnMrzDocumentStatusListener);

		// Once our callback is registered we may now open the reader.
		mBiometricsManager.openMRZ(new Biometrics.MRZStatusListener() {
			@Override
			public void onMRZOpen(Biometrics.ResultCode resultCode) {
				if (resultCode != OK) {
					Log.w(localTAG, "OpenMRZReader: FAILED");

					mStatusTextView.setText(getString(R.string.mrz_open_failed));
					return;
				}

				mIsMRZOpen = true;

				mStatusTextView.setText(getString(R.string.mrz_opened));
				mOpenMRZButton.setText(getString(R.string.close_mrz));
				mOpenRFButton.setEnabled(true);
			}

			@Override
			public void onMRZClose(Biometrics.ResultCode resultCode,
								   Biometrics.CloseReasonCode closeReasonCode) {
				Log.d(localTAG, "MRZ reader closed: " + closeReasonCode.name());

				mIsMRZOpen = false;

				mStatusTextView.setText(getString(R.string.mrz_closed));
				mOpenMRZButton.setText(getString(R.string.open_mrz));

				mOpenRFButton.setEnabled(false);
				mOpenRFButton.setText(getString(R.string.open_epassport));
			}
		});
	}

	private void
	openEPassportReader() {
		final String localTAG = TAG + ":openEPassportReader";
		Log.d(localTAG, "openEPassportReader()");

		mStatusTextView.setText(getString(R.string.epassport_opening));

		// Register a listener will be invoked each time EPassport reader's status changes. Meaning
		// that anytime a document is placed/removed invoke this callback.
		mBiometricsManager.registerEpassportCardStatusListener(mOnEpassportCardStatusListener);

		// Once our callback is registered we may now open the reader.
		mBiometricsManager.ePassportOpenCommand(new Biometrics.EpassportReaderStatusListener() {
			@Override
			public void onEpassportReaderOpen(Biometrics.ResultCode resultCode) {
				if (resultCode == FAIL) {
					Log.w(localTAG, "OpenEPassport: FAILED");

					mStatusTextView.setText(getString(R.string.epassport_open_failed));
					return;
				}

				mIsEpassportOpen = true;

				mOpenRFButton.setText(getString(R.string.close_epassport));
				mStatusTextView.setText(getString(R.string.epassport_opened));
			}

			@Override
			public void onEpassportReaderClosed(Biometrics.ResultCode resultCode,
												Biometrics.CloseReasonCode closeReasonCode) {
				Log.d(localTAG, "EPassport reader closed: " + closeReasonCode.name());

				mIsEpassportOpen = false;

				mOpenRFButton.setEnabled(true);
				mOpenRFButton.setText(getString(R.string.open_epassport));
				mStatusTextView.setText(getString(R.string.epassport_closed));
			}
		});
	}

	private void
	readICAODocument(String dateOfBirth,
					 String documentNumber,
					 String dateOfExpiry) {
		final String localTAG = TAG + ":readICAODocument";
		Log.d(localTAG, "readICAODocument()");

		if (dateOfBirth == null || dateOfBirth.isEmpty()) {
			Log.w(localTAG, "DateOfBirth parameter INVALID, will not read ICAO document.");
			return;
		}
		if (documentNumber == null || documentNumber.isEmpty()) {
			Log.w(localTAG, "DocumentNumber parameter INVALID, will not read ICAO document.");
			return;
		}
		if (dateOfExpiry == null || dateOfExpiry.isEmpty()) {
			Log.w(localTAG, "DateOfExpiry parameter INVALID, will not read ICAO document.");
			return;
		}

		// DateOfBirth and DateOfExpiry must be in ""YYMMDD" format.
		mBiometricsManager.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry,
				(Biometrics.ResultCode resultCode,
				 ICAOReadIntermediateCode stage,
				 String hint,
				 ICAODocumentData icaoDocumentData) -> {
					Log.d(localTAG, "STAGE: " + stage.name()
							+ ", Status: "
							+ resultCode.name()
							+ "Hint: " + hint);
					Log.d(localTAG, "ICAODocumentData: " + icaoDocumentData.toString());

					// Display ICAODocumentData to UI for user to see.
					mICAOTextView.setText(icaoDocumentData.toString());

					// If DG2 state was successful then display read face image to ImageView.
					if (stage == ICAOReadIntermediateCode.DG2 && resultCode == OK)
						mICAOImageView.setImageBitmap(icaoDocumentData.dgTwo.faceImage);
				});
	}
}
