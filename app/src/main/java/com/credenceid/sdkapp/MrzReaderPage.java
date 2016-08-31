package com.credenceid.sdkapp;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.MRZStatusListener;
import com.credenceid.biometrics.Biometrics.OnMrzDocumentStatusListener;
import com.credenceid.biometrics.Biometrics.OnEpassportCardStatusListener;
import com.credenceid.biometrics.Biometrics.OnMrzReadListener;
import com.credenceid.biometrics.Biometrics.ResultCode;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MrzReaderPage extends LinearLayout implements PageView {
	private static final String TAG = MrzReaderPage.class.getName();

    private SampleActivity mActivity;
	private Biometrics mBiometrics;
	private Button mMrzReadBtn;
	private Button mMrzOpenCloseBtn;
	private Button mMrzRfReadBtn;
	private TextView mStatusTextView;
	private Boolean mMrzConnected=false;
	private static int close_cmd_counter =0;
	private static int open_cmd_counter =0;
	private int mCallbackCount=0;
	

	private static String[] mApduList= {
				"00A4040C07A0000002471001", "Select LDS"
				//"00A4020C023F00", "Get Challenge"
				//"0084000008", "Select MF"
				};
	
	private int mApduIdx=0;

	private String mProductName;		
	
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

	private void initialize() {
		Log.d(TAG, "initialize");
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_mrz_reader, this, true);

		mMrzOpenCloseBtn = (Button) findViewById(R.id.open_close);
		mMrzOpenCloseBtn.setText("Open");
		mMrzOpenCloseBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mMrzOpenCloseBtn.setEnabled(false);
				if(mMrzOpenCloseBtn.getText().toString().equalsIgnoreCase("Open"))
				{
					doEnable();
				}
				else
				{
					doClose();
					
				}
			}
		});
		
		mMrzReadBtn = (Button) findViewById(R.id.mrz_read_button);
		mMrzReadBtn.setEnabled(false);
		mMrzReadBtn.setText("Activate MRZ");
		mMrzReadBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				doRead();
			}
		});
		
		
		mMrzRfReadBtn = (Button) findViewById(R.id.mrz_RF_button);
		//mMrzRfReadBtn.setEnabled(false);
		mMrzRfReadBtn.setText("Open RF");
		mMrzRfReadBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(mMrzRfReadBtn.getText().toString().equalsIgnoreCase("Open RF"))
				{
					doEpassportOpen();
				}
				else
				{
					doEpassportClose();
				}
			}
		});
		
		
		
		mStatusTextView = (TextView) findViewById(R.id.mrz_status_textView);

	}
	
	public void setActivity(SampleActivity activity) {
		mActivity = activity;
		 mProductName = activity.getProductName();
		Log.d(TAG, "Product Name="+mProductName);
		if ( mProductName.equalsIgnoreCase("starlight"))
			mMrzRfReadBtn.setVisibility(VISIBLE);
		else
			mMrzRfReadBtn.setVisibility(GONE);
	}

	@Override
	public String getTitle() {
		return getContext().getResources().getString(R.string.mrz_page_title);
	}

	@Override
	public void activate(Biometrics biometrics) {
		mBiometrics = biometrics;
		mStatusTextView.setText("");
		//mMrzReadBtn.setEnabled(true);
	}

	public void doResume() {
		if(!mMrzConnected)
		{
			Log.d(TAG, "doResume: No connected, doEnable()");
			//doEnable();
		}
		else
			Log.d(TAG, "doResume: Already connected to MRZ, do nothing");

	}

	@Override
	public void deactivate() {
	}

	private void doEnable() {
		
		Log.d(TAG, "Calling openMRZ");
		mStatusTextView.setText("Opening MRZ Reader");
		open_cmd_counter =0;
		mBiometrics.openMRZ(new MRZStatusListener(){

			@Override
			public void onMRZOpen(ResultCode resultCode) {
				open_cmd_counter++;
				Log.d(TAG, "onMRZOpen: resultCode: " + resultCode.name());
				mStatusTextView.setText("MRZ Open:"+resultCode.name());
				if(resultCode==ResultCode.OK)
				{
					mMrzOpenCloseBtn.setEnabled(true);
					mMrzOpenCloseBtn.setText("Close");
					mMrzReadBtn.setEnabled(true);
					mBiometrics.registerMrzReadListener(mrzReadListener);
					mBiometrics.registerMrzDocumentStatusListener(mrzDocumentStatusListener);
				}
			}

			@Override
			public void onMRZClose(CloseReasonCode resultCode) {
				close_cmd_counter++;
				Log.d(TAG, "onMRZClose ");
				mMrzOpenCloseBtn.setEnabled(true);
				mMrzOpenCloseBtn.setText("Open");
				mMrzReadBtn.setEnabled(false);
				mStatusTextView.setText("MRZ Closed:"+resultCode.toString());
			}
			
		});
	}
	
	private void doClose(){
		close_cmd_counter=0;
		Log.d(TAG, "Calling CloseMRZ");
		mStatusTextView.setText("Closing MRZ Reader");
		mMrzReadBtn.setEnabled(false);
		mBiometrics.closeMRZ();
	}
	
	private OnMrzDocumentStatusListener mrzDocumentStatusListener = new OnMrzDocumentStatusListener() {
		
		@Override
		public void onMrzDocumentStatusChange(int arg0, int arg1) {
			// TODO Auto-generated method stub
			if(arg1==2)
			{
				mStatusTextView.setText("MRZ document present");
				
			}
			else
				mStatusTextView.setText("MRZ document absent");
			
		}
	};
	
	private OnMrzReadListener mrzReadListener = new OnMrzReadListener() {
		
		@Override
		public void onMrzRead(ResultCode result, String hint,  byte[] rawData, String stringData,
				String parsedStringData) {
			
			Log.d(TAG, "onMrzRead:Received Call back from Credence Service, hint: " + hint + ", stringData: " + stringData + ", parsedStringData: " + parsedStringData);
			String statusText="";

			if(result == ResultCode.FAIL)
			{
				statusText="";
				statusText+="Result: FAIL. \n ";
				
				if (rawData!=null)
				{
					statusText+="Raw Data Length:"+rawData.length+".  \n ";
				}
				if(stringData!=null)
				{
					statusText+="Raw String Data :"+stringData+".  \n ";
				}
					
				mStatusTextView.setText(statusText);
				mMrzReadBtn.setEnabled(true);
				mMrzConnected = false;

			}
			else if (result == ResultCode.INTERMEDIATE)
			{
				mMrzConnected = true;
				mMrzReadBtn.setEnabled(false);
				mStatusTextView.setText("INTERMEDIATE:"+hint);
			}
			else if (result == ResultCode.OK)
			{
				mMrzConnected = false;
				statusText="";
				statusText+="Result: OK. \n ";


				if (rawData!=null)
				{
					statusText+="Raw Data Length:"+rawData.length+". \n ";
				}
				if(parsedStringData==null || parsedStringData.isEmpty())
				{
					statusText+="Raw Data:"+stringData+". \n";
				}
				else
				{
					statusText+="Parsed Data:"+parsedStringData+". \n ";
				}
				mStatusTextView.setText(statusText);
				mMrzReadBtn.setEnabled(true);
			}
			else
			{
				mStatusTextView.setText("UNKNOWN RESULT");
				mMrzReadBtn.setEnabled(true);		
				mMrzConnected = false;

			}
		}
	};
 
	private void doEpassportOpen()
	{
		mCallbackCount=0;
		mBiometrics.registerEpassportCardStatusListener(epassCardStatusListener);
		mBiometrics.ePassportOpenCommand(new Biometrics.EpassportReaderStatusListner() {
			
			@Override
			public void onEpassportReaderOpen(ResultCode rc) {
				
				mMrzRfReadBtn.setText("Close RF");
				// TODO Auto-generated method stub
				mStatusTextView.setText("ePassport Reader Open Result:"+rc.toString());
				if (rc == ResultCode.OK)
				{
					mStatusTextView.setText("ePassport Reader Open :"+mCallbackCount++);
					//doEpassportTransmit();
					
				}
			}
			
			@Override
			public void onEpassportReaderClosed(CloseReasonCode rc) {
				mMrzRfReadBtn.setText("Open RF");
				mStatusTextView.setText("ePassport Reader Close Result:"+rc.toString());
			}
		});
	}
	
	private void doEpassportClose()
	{
		mBiometrics.registerEpassportCardStatusListener(null);
		mBiometrics.ePassportCloseCommand();
		mMrzRfReadBtn.setText("Open RF");
	}
	
	public void doEpassportTransmit()
	{
		for(int i=0; i<mApduList.length; i=+2)
		{
			ApduCommand APDU = new ApduCommand(mApduList[i]);
			//ApduCommand APDU = new ApduCommand("0084000008");
			
			mBiometrics.ePassportCommand(APDU, true, new Biometrics.OnEpassportCommandListener() {
				
				@Override
				public void onEpassportCommandComplete(ResultCode arg0, byte arg1,
						byte arg2, byte[] data) {
					// TODO Auto-generated method stub
					String ds;
					if (arg0 == ResultCode.OK) {
						if (data==null||data.length == 0) {
							ds = new String("{no data}");
						} else {
							int di;
							ds = new String("");
							for (di = 0; di < data.length; di++)
								ds = ds
										+ new String(String.format("%02X",
												(0x0ff) & data[di]));
						}
						
						mStatusTextView.setText("ePassport Responce: " + arg0.name() + " " +
								"SW1,SW2: " +
								String.format("%02x", (0x0ff) & arg1) +
								String.format("%02x", (0x0ff) & arg2) +
								" D: " + ds);
					
						
				}
					else
						mStatusTextView.setText("Result Code:"+arg0.toString());
				}
				
				});
		}
	}
	
	private OnEpassportCardStatusListener epassCardStatusListener = new OnEpassportCardStatusListener() {
		
		@Override
		public void onEpassportCardStatusChange(int arg0, int arg1) {
			// TODO Auto-generated method stub
			if(arg1==2)
			{
				mStatusTextView.setText("Epassport present");
				doEpassportTransmit();
			}
			else
				mStatusTextView.setText("Epassport absent");
			
		}
	};


	private void doRead() {
	
		mBiometrics.readMRZ(mrzReadListener);
		mStatusTextView.setText("Ready to read...");
	}

	private void doDisable() {
		mStatusTextView.setText("");
		mBiometrics.cancelCapture();
	}
}
