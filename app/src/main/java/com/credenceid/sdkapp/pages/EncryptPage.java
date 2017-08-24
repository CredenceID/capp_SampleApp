package com.credenceid.sdkapp.pages;

import java.math.BigInteger;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Crypto;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.models.PageView;

public class EncryptPage extends LinearLayout implements PageView {
	private static final String TAG = EncryptPage.class.getName();

	private Button mGenerateBtn;
	private Button mEncryptBtn;
	private Button mDecryptBtn;
	private EditText mPassphraseEditText;
	private EditText mKeyEditText;
	private EditText mPayloadEditText;
	private TextView mStatusTextView;
	private EditText mCurrentEditText;

	public EncryptPage(Context context) {
		super(context);
		initialize();
	}

	public EncryptPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public EncryptPage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {
		Log.d(TAG, "initialize");
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_encrypt, this, true);

		Button randomBtn = (Button) findViewById(R.id.random_btn);
		randomBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onRandom(v);
			}
		});

		mGenerateBtn = (Button) findViewById(R.id.generate_btn);
		mGenerateBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onGenerate(v);
			}
		});
		mEncryptBtn = (Button) findViewById(R.id.encrypt_btn);
		mEncryptBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onEncrypt(v);
			}
		});
		mDecryptBtn = (Button) findViewById(R.id.decrypt_btn);
		mDecryptBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onDecrypt(v);
			}
		});
		mPassphraseEditText = (EditText) findViewById(R.id.passphrase_text);
		mPassphraseEditText.setHorizontallyScrolling(false);
		mPassphraseEditText.setMaxLines(Integer.MAX_VALUE);
		mPassphraseEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				mCurrentEditText = mPassphraseEditText;
				String content = mPassphraseEditText.getText().toString();
				mGenerateBtn.setEnabled(!content.isEmpty());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}
		});
		mKeyEditText = (EditText) findViewById(R.id.key_text);
		mKeyEditText.setHorizontallyScrolling(false);
		mKeyEditText.setMaxLines(Integer.MAX_VALUE);
		mKeyEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				mCurrentEditText = mKeyEditText;
				String content = mKeyEditText.getText().toString();
				mEncryptBtn.setEnabled(!content.isEmpty());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}
		});
		mPayloadEditText = (EditText) findViewById(R.id.payload_text);
		mPayloadEditText.setHorizontallyScrolling(false);
		mPayloadEditText.setMaxLines(Integer.MAX_VALUE);
		mPayloadEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				mCurrentEditText = mPayloadEditText;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}
		});

		mStatusTextView = (TextView) findViewById(R.id.status);

	}

	@Override
	public String getTitle() {
		return getContext().getResources().getString(R.string.title_encryption);
	}

	@Override
	public void activate(Biometrics biometrics) {
		doResume();
	}

	public void doResume() {
		mPassphraseEditText.setText("");
		mKeyEditText.setText("");
		mPayloadEditText.setText("This text will be encrypted");
		mStatusTextView.setText("");

		mGenerateBtn.setEnabled(false);
		mEncryptBtn.setEnabled(false);
		mDecryptBtn.setEnabled(false);
	}

	@Override
	public void deactivate() {
	}

	// Generates a key from a passphrase
	private void onGenerate(View v) {
		hideSoftKeyboard();
		String phrase = mPassphraseEditText.getText().toString();
		byte[] key_bytes = Crypto.aesKeyFromPhrase(phrase);
		mKeyEditText.setText(bytesToHexString(key_bytes));
		mStatusTextView.setText("Key generated from passphrase");
	}

	// Generates a random key
	private void onRandom(View v) {
		hideSoftKeyboard();
		byte[] key_bytes = Crypto.aesRandomKey();
		mKeyEditText.setText(bytesToHexString(key_bytes));
		mStatusTextView.setText("Random key created");
	}

	// Encrypt text
	public void onEncrypt(View v) {
		hideSoftKeyboard();
		byte[] key_bytes = hexStringToBytes(mKeyEditText.getText().toString());
		if (key_bytes == null) {
			mStatusTextView.setText("Invalid key");
		}
		byte[] encrypted_bytes = Crypto.aesEncrypt(key_bytes, mPayloadEditText.getText().toString());
		if (encrypted_bytes == null) {
			mStatusTextView.setText("Encryption failed");
		} else {
			mPayloadEditText.setText(bytesToHexString(encrypted_bytes));
			mStatusTextView.setText("Data encrypted");
			mEncryptBtn.setEnabled(false);
			mDecryptBtn.setEnabled(true);
		}
	}

	// Decrypt text
	private void onDecrypt(View v) {
		hideSoftKeyboard();
		mEncryptBtn.setEnabled(true);
		mDecryptBtn.setEnabled(true);

		byte[] key_bytes = hexStringToBytes(mKeyEditText.getText().toString());
		byte[] encrypted_bytes = hexStringToBytes(mPayloadEditText.getText().toString());
		if (key_bytes == null) {
			mStatusTextView.setText("Invalid key");
		} else if (encrypted_bytes == null) {
			mStatusTextView.setText("Invalid encrypted text");
		} else {
			String decrypted = Crypto.aesDecryptToString(key_bytes, encrypted_bytes);
			if (decrypted == null) {
				mStatusTextView.setText("Decryption failed");
			} else {
				mPayloadEditText.setText(decrypted);
				mStatusTextView.setText("Data decrypted");
				mDecryptBtn.setEnabled(false);
			}
		}
	}

	// Convert bytes to HexString
	private static String bytesToHexString(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		String str = String.format("%0" + (bytes.length << 1) + "X", bi);
		return str;
	}

	// convert String to bytes
	public static byte[] hexStringToBytes(String s) {
		int len = s.length();
		if (len == 0 || len % 2 != 0)
			return null;
		if (!s.matches("[\\dA-Fa-f]+"))
			return null;
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(
					s.charAt(i + 1), 16));
		}
		return data;
	}

	// hides keyboard
	private void hideSoftKeyboard() {
		if (mCurrentEditText != null) {
			InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
					Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mCurrentEditText.getWindowToken(), 0);
		}
	}
}
