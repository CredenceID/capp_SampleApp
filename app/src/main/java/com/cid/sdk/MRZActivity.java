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

	private static final int mDOCUMENT_PRESENT_CODE = 2;

	private final int mMRZ_DATA_COUNT = 10;
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

	/* Callback invoked each time MRZ reader is able to read MRZ text from document. */
	@SuppressWarnings("SpellCheckingInspection")
	private Biometrics.OnMrzReadListener mOnMrzReadListener = (Biometrics.ResultCode resultCode,
															   String hint,
															   byte[] rawData,
															   String data,
															   String parsedData) -> {

		Log.d(TAG, "OnMrzReadListener: Hit: " + hint);
		Log.d(TAG, "OnMrzReadListener: ResultCode: " + resultCode.name());
		Log.d(TAG, "OnMrzReadListener: Data: " + data);
		Log.d(TAG, "OnMrzReadListener: ParsedData: " + parsedData);

		if (FAIL == resultCode)
			mStatusTextView.setText(getString(R.string.mrz_failed_reswipe));
		else if (INTERMEDIATE == resultCode)
			mStatusTextView.setText(getString(R.string.mrz_reading_wait));
		else {
			/* Once data is read, it is auto parsed and returned as one big string of data. */
			if (null == parsedData || parsedData.isEmpty()) {
				mStatusTextView.setText(getString(R.string.mrz_failed_reswipe));
				return;
			}

			/* Each section of data is separated by a "\r\n" character. If we split this data up, we
			 * should have TEN elements of data. Please see the constants defined at the top of this
			 * class to see the different pieces of information MRZ contains and their respective
			 * indexes.
			 */
			final String[] splitData = parsedData.split("\r\n");
			if (splitData.length < mMRZ_DATA_COUNT) {
				mStatusTextView.setText(getString(R.string.mrz_failed_reswipe));
				return;
			}
			/* Only if it returned back appropriate number of data peaces do we move forward. */

			mStatusTextView.setText(getString(R.string.mrz_read_success));
			mICAOTextView.setText(parsedData);

			/* Now try to read ICAO document with information from MRZ. */
			readICAODocument(splitData[mDATE_OF_BIRTH],
					splitData[mDOCUMENT_NUMBER],
					splitData[mDATE_OF_EXPIRY]);
		}
	};

	/* Callback invoked each time C-Service detects a document change from MRZ reader. */
	private Biometrics.OnMrzDocumentStatusListener mOnMrzDocumentStatusListener
			= (int previousState, int currentState) -> {

		/* If currentState is not 2, then no document is present. */
		if (mDOCUMENT_PRESENT_CODE != currentState) {
			Log.d(TAG, "OnMrzDocumentStatusListener: No document present.");
			return;
		}

		mStatusTextView.setText(getString(R.string.mrz_reading_wait));

		/* If current state is 2, then a document is present on MRZ reader. If a document
		 * is present we must read it to obtain MRZ field data. Call "readMRZ" to read the document.
		 *
		 * When MRZ is read this callback is invoked "mOnMrzReadListener".
		 */
		mBiometricsManager.readMRZ(mOnMrzReadListener);
	};

	/* Callback invoked each time sensor detects a document change from EPassport reader. */
	private Biometrics.OnEpassportCardStatusListener mOnEpassportCardStatusListener
			= (int previousState, int currentState) -> {

		/* If currentState is not 2, then no document is present. */
		if (mDOCUMENT_PRESENT_CODE != currentState)
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

		/* If back button is pressed when we want to destroy activity. */
		this.onDestroy();
	}

	@Override
	protected void
	onDestroy() {
		super.onDestroy();

		/* If user presses back button then close all open peripherals. */
		mBiometricsManager.ePassportCloseCommand();
		mBiometricsManager.closeMRZ();

		/* If user presses back button then they are exiting application. If this is the case then
		 * tell C-Service to unbind from this application.
		 */
		mBiometricsManager.finalizeBiometrics(false);
	}

	/* Initializes all objects inside layout file. */
	private void
	initializeLayoutComponents() {
		mStatusTextView = findViewById(R.id.status_textview);

		mICAOImageView = findViewById(R.id.icao_dg2_imageview);
		mICAOTextView = findViewById(R.id.icao_textview);

		mOpenMRZButton = findViewById(R.id.open_mrz_button);
		mOpenRFButton = findViewById(R.id.open_epassport_buton);
	}

	/* Configure all objects in layout file, set up listeners, views, etc. */
	private void
	configureLayoutComponents() {
		mOpenMRZButton.setEnabled(true);
		mOpenMRZButton.setText(getString(R.string.open_mrz));
		mOpenMRZButton.setOnClickListener((View v) -> {
			/* Based on current state of MRZ reader take appropriate action. */
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
			/* Based on current state of EPassport reader take appropriate action. */
			if (!mIsEpassportOpen)
				openEPassportReader();
			else mBiometricsManager.ePassportCloseCommand();
		});
	}

	/* Calls Credence APIs to open MRZ reader. */
	private void
	openMRZReader() {
		mStatusTextView.setText(getString(R.string.mrz_opening));

		/* Register a listener that will be invoked each time MRZ reader's status changes. Meaning
		 * that anytime a document is placed/removed invoke this callback.
		 */
		mBiometricsManager.registerMrzDocumentStatusListener(mOnMrzDocumentStatusListener);

		/* Once our callback is registered we may now open the reader. */
		mBiometricsManager.openMRZ(new Biometrics.MRZStatusListener() {
			@Override
			public void onMRZOpen(Biometrics.ResultCode resultCode) {
				if (OK != resultCode) {
					mStatusTextView.setText(getString(R.string.mrz_open_failed));
					return;
				}

				/* Now that sensor is open, if user presses "mOpenMRZButton" sensor should now
				 * close. To achieve this we change flag which controls what action button takes.
				 */
				mIsMRZOpen = true;

				mStatusTextView.setText(getString(R.string.mrz_opened));
				mOpenMRZButton.setText(getString(R.string.close_mrz));
				mOpenRFButton.setEnabled(true);
			}

			@Override
			public void onMRZClose(Biometrics.ResultCode resultCode,
								   Biometrics.CloseReasonCode closeReasonCode) {
				/* Now that sensor is open, if user presses "mOpenMRZButton" sensor should now
				 * open. To achieve this we change flag which controls what action button takes.
				 */
				mIsMRZOpen = false;

				mStatusTextView.setText(getString(R.string.mrz_closed));
				mOpenMRZButton.setText(getString(R.string.open_mrz));

				mOpenRFButton.setEnabled(false);
				mOpenRFButton.setText(getString(R.string.open_epassport));
			}
		});
	}

	/* Calls Credence APIs to open EPassport reader. */
	private void
	openEPassportReader() {
		mStatusTextView.setText(getString(R.string.epassport_opening));

		/* Register a listener will be invoked each time EPassport reader's status changes. Meaning
		 * that anytime a document is placed/removed invoke this callback.
		 */
		mBiometricsManager.registerEpassportCardStatusListener(mOnEpassportCardStatusListener);

		/* Once our callback is registered we may now open the reader. */
		mBiometricsManager.ePassportOpenCommand(new Biometrics.EpassportReaderStatusListener() {
			@Override
			public void onEpassportReaderOpen(Biometrics.ResultCode resultCode) {
				if (FAIL == resultCode) {
					mStatusTextView.setText(getString(R.string.epassport_open_failed));
					return;
				}

				/* Now that sensor is open, if user presses "mOpenRFButton" sensor should now
				 * close. To achieve this we change flag which controls what action button takes.
				 */
				mIsEpassportOpen = true;

				mOpenRFButton.setText(getString(R.string.close_epassport));
				mStatusTextView.setText(getString(R.string.epassport_opened));
			}

			@Override
			public void onEpassportReaderClosed(Biometrics.ResultCode resultCode,
												Biometrics.CloseReasonCode closeReasonCode) {
				/* Now that sensor is open, if user presses "mOpenRFButton" sensor should now
				 * close. To achieve this we change flag which controls what action button takes.
				 */
				mIsEpassportOpen = false;

				mOpenRFButton.setEnabled(true);
				mOpenRFButton.setText(getString(R.string.open_epassport));
				mStatusTextView.setText(getString(R.string.epassport_closed));
			}
		});
	}

	/* Calls Credence APIs to read an ICAO document.
	 *
	 * @param dateOfBirth Date of birth on ICAO document (YYMMDD format).
	 * @param documentNumber Document number of ICAO document.
	 * @param dateOfExpiry Date of expiry on ICAO document (YYMMDD format).
	 */
	@SuppressWarnings("SpellCheckingInspection")
	private void
	readICAODocument(String dateOfBirth,
					 String documentNumber,
					 String dateOfExpiry) {

		/* If any one of three parameters is bad then do not proceed with document reading. */
		if (null == dateOfBirth || dateOfBirth.isEmpty()) {
			Log.w(TAG, "DateOfBirth parameter INVALID, will not read ICAO document.");
			return;
		}
		if (null == documentNumber || documentNumber.isEmpty()) {
			Log.w(TAG, "DocumentNumber parameter INVALID, will not read ICAO document.");
			return;
		}
		if (null == dateOfExpiry || dateOfExpiry.isEmpty()) {
			Log.w(TAG, "DateOfExpiry parameter INVALID, will not read ICAO document.");
			return;
		}

		mBiometricsManager.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry,
				(Biometrics.ResultCode resultCode,
				 ICAOReadIntermediateCode stage,
				 String hint,
				 ICAODocumentData icaoDocumentData) -> {

					Log.d(TAG, "STAGE: " + stage.name()
							+ ", Status: "
							+ resultCode.name()
							+ "Hint: " + hint);
					Log.d(TAG, "ICAODocumentData: " + icaoDocumentData.toString());

					/* Display ICAODocumentData to UI for user to see. */
					mICAOTextView.setText(icaoDocumentData.toString());

					/* If DG2 state was successful then display read face image to ImageView. */
					if (ICAOReadIntermediateCode.DG2 == stage && OK == resultCode)
						mICAOImageView.setImageBitmap(icaoDocumentData.dgTwo.faceImage);
				});
	}
}
