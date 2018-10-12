package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.MRZStatusListener;
import com.credenceid.biometrics.Biometrics.OnEpassportCardStatusListener;
import com.credenceid.biometrics.Biometrics.OnMrzDocumentStatusListener;
import com.credenceid.biometrics.Biometrics.OnMrzReadListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.icao.ICAODocumentData;
import com.credenceid.icao.ICAOReadIntermediateCode;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.activity.SampleActivity;
import com.credenceid.sdkapp.models.PageView;

import static com.credenceid.biometrics.Biometrics.ResultCode.API_UNAVAILABLE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.icao.ICAOReadIntermediateCode.BAC_SUCCESS;
import static com.credenceid.icao.ICAOReadIntermediateCode.DG1;
import static com.credenceid.icao.ICAOReadIntermediateCode.DG2;
import static com.credenceid.icao.ICAOReadIntermediateCode.DG3;

@SuppressWarnings({"SpellCheckingInspection"})
public class MrzReaderPage extends LinearLayout implements PageView {
	private static final String TAG = MrzReaderPage.class.getSimpleName();

	// Keep track of how many times MRZ/EPassport sensor has been closed.
	private static int close_cmd_counter = 0;
	// Keep track of how many times MRZ/EPassport sensor has been opened.
	private static int open_cmd_counter = 0;

	private static String[] mApduList = {
			"00A4040C07A0000002471001", "Select LDS"
			//"00A4020C023F00", "Get Challenge"
			//"0084000008", "Select MF"
	};

	private Biometrics mBiometrics;

	private Button mMrzReadBtn;
	private Button mMrzOpenCloseBtn;
	private Button mMrzRfReadBtn;
	private TextView mStatusTextView;

	private Boolean mMrzConnected = false;

	// Keep track of how many times callback has been invoked.
	private int mCallbackCount = 0;

	// Listener for MRZ Document Status callback
	private OnMrzDocumentStatusListener mrzDocumentStatusListener = (int arg0, int arg1) ->
			setStatusText("MRZ document " + (arg1 == 2 ? "present" : "absent"));

	// Listener for MRZ Read callback. This is where the data on card will come back to
	private OnMrzReadListener mrzReadListener = (ResultCode result,
												 String hint,
												 byte[] rawData,
												 String stringData,
												 String parsedStringData) -> {

		Log.d(TAG, "onMrzRead:Received Call back from Credence Service, hint: " + hint
				+ ", stringData: " + stringData
				+ ", parsedStringData: " + parsedStringData);
		String statusText;

		if (result == ResultCode.FAIL) {
			statusText = "";
			statusText += "Result: FAIL. \n ";

			if (rawData != null)
				statusText += "Raw Data Length:" + rawData.length + ".  \n ";
			if (stringData != null)
				statusText += "Raw String Data :" + stringData + ".  \n ";

			setStatusText(statusText);
			mMrzReadBtn.setEnabled(true);
			mMrzConnected = false;
		} else if (result == ResultCode.INTERMEDIATE) {
			mMrzConnected = true;
			mMrzReadBtn.setEnabled(false);
			setStatusText("INTERMEDIATE: " + hint);
		} else if (result == ResultCode.OK) {
			mMrzConnected = false;
			statusText = "";
			statusText += "Result: OK. \n ";

			if (rawData != null)
				statusText += "Raw Data Length:" + rawData.length + ". \n ";

			if (parsedStringData == null || parsedStringData.isEmpty())
				statusText += "Raw Data:" + stringData + ". \n";
			else statusText += "Parsed Data:" + parsedStringData + ". \n ";

			setStatusText(statusText);
			mMrzReadBtn.setEnabled(true);
		} else {
			setStatusText("UNKNOWN RESULT");
			mMrzReadBtn.setEnabled(true);
			mMrzConnected = false;
		}
	};

	// Listener for the ePassport Card Status
	private OnEpassportCardStatusListener epassCardStatusListener = (int arg0, int arg1) -> {
		if (arg1 == 2) {
			setStatusText("Epassport present, will attempt ICAO document read...");
			this.doReadICAODocument();
		} else setStatusText("Epassport absent");
	};

	public MrzReaderPage(Context context) {
		super(context);
		initialize();
	}

	public MrzReaderPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public MrzReaderPage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	@SuppressWarnings({"all"})
	private void
	initialize() {
		Log.d(TAG, "initialize");
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_mrz_reader, this, true);

		mMrzOpenCloseBtn = (Button) findViewById(R.id.open_close);
		mMrzOpenCloseBtn.setText("Open");
		mMrzOpenCloseBtn.setOnClickListener((View v) -> {
			mMrzOpenCloseBtn.setEnabled(false);
			if (mMrzOpenCloseBtn.getText().toString().equalsIgnoreCase("Open"))
				doOpen();
			else doClose();
		});

		mMrzReadBtn = (Button) findViewById(R.id.mrz_read_button);
		mMrzReadBtn.setEnabled(false);
		mMrzReadBtn.setText("Activate MRZ");
		mMrzReadBtn.setOnClickListener((View v) -> doRead());


		mMrzRfReadBtn = (Button) findViewById(R.id.mrz_RF_button);
		mMrzRfReadBtn.setEnabled(false);
		mMrzRfReadBtn.setText("Open RF");
		mMrzRfReadBtn.setOnClickListener((View v) -> {
			if (mMrzRfReadBtn.getText().toString().equalsIgnoreCase("Open RF"))
				doEpassportOpen();
			else doEpassportClose();
		});

		mStatusTextView = (TextView) findViewById(R.id.mrz_status_textView);
	}

	public void
	setActivity(SampleActivity activity) {
		String productName = activity.getProductName();
		Log.d(TAG, "Product Name= " + productName);

		if (productName.equalsIgnoreCase("starlight")
				|| productName.equalsIgnoreCase("credence tab")
				|| productName.equalsIgnoreCase("Credence TAB V3")
				|| productName.equalsIgnoreCase("Credence TAB V4")
				|| productName.equalsIgnoreCase("Credence One V3")
				|| productName.equalsIgnoreCase("CredenceOne-V3")) {
			mMrzRfReadBtn.setVisibility(VISIBLE);
		} else mMrzRfReadBtn.setVisibility(GONE);
	}

	@Override
	public String
	getTitle() {
		return getContext().getResources().getString(R.string.mrz_page_title);
	}

	@Override
	public void
	activate(Biometrics biometrics) {
		mBiometrics = biometrics;
	}

	@Override
	public void
	deactivate() {
	}

	public void
	doResume() {
		if (!mMrzConnected) {
			Log.d(TAG, "doResume: No connected, doOpen()");
			//doOpen();
		} else Log.d(TAG, "doResume: Already connected to MRZ, do nothing");
	}

	// When called this will call the Open MRZ API
	private void
	doOpen() {
		Log.d(TAG, "Calling openMRZ");
		setStatusText("Opening MRZ Reader");
		open_cmd_counter = 0;

		mBiometrics.openMRZ(new MRZStatusListener() {
			@Override
			public void onMRZOpen(ResultCode resultCode) {
				open_cmd_counter++;
				Log.d(TAG, "onMRZOpen opened-" + open_cmd_counter + ", " + resultCode.name());
				setStatusText("MRZ Open: " + resultCode.name());

				if (resultCode == ResultCode.OK) {
					updateButtons(true);
					mBiometrics.registerMrzReadListener(mrzReadListener);
					mBiometrics.registerMrzDocumentStatusListener(mrzDocumentStatusListener);
				} else if (resultCode == ResultCode.FAIL)
					updateButtons(false);
			}

			@Override
			public void onMRZClose(ResultCode resultCode, CloseReasonCode closeCode) {
				if (resultCode == ResultCode.OK) {
					close_cmd_counter++;
					Log.d(TAG, "onMRZClose-" + close_cmd_counter);
					setStatusText("MRZ Closed: " + closeCode.toString());
					updateButtons(false);
				} else if (resultCode == ResultCode.FAIL) {
					Log.d(TAG, "onMRZClose: FAILED");
					setStatusText("MRZ Closed: FAILED");
					mMrzRfReadBtn.setEnabled(true);
					mMrzReadBtn.setEnabled(true);
				}
			}

		});
	}

	// Calls the MRZ Close api
	private void
	doClose() {
		close_cmd_counter = 0;
		Log.d(TAG, "Calling CloseMRZ");
		setStatusText("Closing MRZ Reader");
		mMrzRfReadBtn.setEnabled(false);
		mMrzReadBtn.setEnabled(false);
		mBiometrics.closeMRZ();
	}

	// Calls readMRZ API
	private void
	doRead() {
		mBiometrics.readMRZ(mrzReadListener);
		setStatusText("Ready to read...");
	}

	// Calls the ePassport Open API Also sets the epassCardStatus Listener
	@SuppressWarnings({"all"})
	private void
	doEpassportOpen() {
		mCallbackCount = 0;
		mBiometrics.registerEpassportCardStatusListener(epassCardStatusListener);
		mBiometrics.ePassportOpenCommand(new Biometrics.EpassportReaderStatusListener() {
			@Override
			public void onEpassportReaderOpen(ResultCode rc) {
				mMrzRfReadBtn.setText("Close RF");
				setStatusText("ePassport Reader Open Result: " + rc.toString());

				if (rc == ResultCode.OK)
					setStatusText("ePassport Reader Open: " + mCallbackCount++);
				else if (rc == ResultCode.FAIL)
					mMrzRfReadBtn.setText("Open RF");
			}

			@Override
			public void onEpassportReaderClosed(ResultCode resultCode, CloseReasonCode rc) {
				if (resultCode == ResultCode.OK) {
					mMrzRfReadBtn.setText("Open RF");
					setStatusText("ePassport Reader Close Result: " + rc.toString());
				} else if (resultCode == ResultCode.FAIL) {
					Log.d(TAG, "onEpassportReaderClosed: FAILED");
					setStatusText("ePassport Reader Close Result: FAILED");
					mMrzRfReadBtn.setText("Close RF");
				}
			}
		});
	}

	// Calls the ePassport Close API
	@SuppressWarnings({"all"})
	private void
	doEpassportClose() {
		mBiometrics.registerEpassportCardStatusListener(null);
		mBiometrics.ePassportCloseCommand();
		mMrzRfReadBtn.setText("Open RF");
	}

	// This loops through fake data in mApduList and calls ePassportCommand API to process it
	@SuppressWarnings({"unused"})
	public void
	doEpassportTransmit() {
		for (int i = 0; i < mApduList.length; i = +2) {
			ApduCommand APDU = new ApduCommand(mApduList[i]);

			mBiometrics.ePassportCommand(APDU, true, (ResultCode arg0,
													  byte arg1,
													  byte arg2,
													  byte[] data) -> {
				StringBuilder ds = new StringBuilder();
				if (arg0 == ResultCode.OK) {
					if (data == null || data.length == 0) {
						ds.append("{no data}");
					} else {
						int di;
						ds.setLength(0);

						for (di = 0; di < data.length; di++)
							ds.append(String.format("%02X", (0x0ff) & data[di]));
					}
					setStatusText("ePassport Responce: " + arg0.name() + " " +
							"SW1,SW2: " +
							String.format("%02x", (0x0ff) & arg1) +
							String.format("%02x", (0x0ff) & arg2) +
							" D: " + ds.toString());
				} else setStatusText("Result Code: " + arg0.toString());
			});
		}
	}

	public void
	doReadICAODocument() {
		StringBuilder text = new StringBuilder();

		mBiometrics.readICAODocument("210696", "ABCD12345", "011025",
				(ResultCode resultCode,
				 ICAOReadIntermediateCode readCode,
				 String hint,
				 ICAODocumentData icaoDocumentData) -> {
					if (resultCode == API_UNAVAILABLE) {
						Log.w(TAG, "readICAODocument(): API UNAVAILABLE");
						setStatusText("API unavailable, please contact Admin.");
						return;
					} else if (readCode == BAC_SUCCESS) {
						text.append("BAC Authentication: ");
						text.append(resultCode == OK ? "Success" : "Failure");
						text.append("\n");
						text.append(hint);
						text.append("\n");
					} else if (readCode == DG1) {
						text.append("DG1 Read: ");
						text.append(resultCode == OK ? "Success" : "Failure");
						text.append("\n");
					} else if (readCode == DG2) {
						text.append("DG2 Read: ");
						text.append(resultCode == OK ? "Success" : "Failure");
						text.append("\n");
					} else if (readCode == DG3) {
						text.append("DG3 Read: ");
						text.append(resultCode == OK ? "Success" : "Failure");
						text.append("\n");
					}

					setStatusText(text.toString());
				});
	}

	// Updates the button states based on parameter
	@SuppressWarnings({"all"})
	private void
	updateButtons(boolean isOpen) {
		mMrzOpenCloseBtn.setEnabled(true);
		mMrzOpenCloseBtn.setText(isOpen ? "Close" : "Open");
		mMrzReadBtn.setEnabled(isOpen);
		mMrzRfReadBtn.setEnabled(isOpen);

		if (!isOpen)
			mMrzRfReadBtn.setText("Open RF");
	}

	// set Status TextView based on parameter
	private void
	setStatusText(String text) {
		// If text passed contains a string
		if (!text.isEmpty())
			// Log output for debugging, passed text
			Log.d(TAG, "setStatusText: " + text);
		mStatusTextView.setText(text);
	}
}
