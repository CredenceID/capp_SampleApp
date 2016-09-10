package com.credenceid.sdkapp;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CardReaderStatusListner;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.OnCardCommandListener;
import com.credenceid.biometrics.Biometrics.OnCardStatusListener;
import com.credenceid.biometrics.Biometrics.ResultCode;

import java.util.Hashtable;

public class CardReaderPage extends LinearLayout implements PageView {
    // Log tag for debugging
    private static final String TAG = CardReaderPage.class.getName();

    private Biometrics mBiometrics;
    TextView status_text_view;
    Button button_view;
    Button button_open_close;

    private static Hashtable cardList = new Hashtable();
    private static int close_cmd_counter = 0;
    private static int open_cmd_counter = 0;

    public static final int APDU_INIT = 1; // ready for first APDU
    public static final int APDU_PROCESS = 2; // process next APDU

    int APDU_state = APDU_INIT;
    int APDU_idx = 0; // index into APDU table
    public String APDU_lastdesc = "{unknown}";
    public String mCardName = "{unknown}";

    // NULL (no card identified) APDU table
    public String[] noAPDU_table = {"", "No APDUs Defined", // NULL APDU
    };

    // Gemalto MPCOS EMV APDU table
    public String[] MPCOS_EMV_APDU_table = {
            "00a4000c023f00", "SELECT MF 3F00", // select master file 0x3f00
            "00a40100020100", "SELECT DF 0100", // select directory 0x0100
            "00a40200020101", "SELECT EF 0101", // select elementary file 0x0101
            "00b000000C", "READ BINARY",      // READ BINARY
    };

    // Payflex 1K APDU table
    public String[] Payflex_1K_APDU_table = {
            "00a40000020002", "SELECT FILE",
            "00b2000008", "READ RECORD",
    };

    // G&D Sm@rtCafe Expert 64
    public String[] GD_SmartCafe_Expert64_APDU_table = {
            "80CA00C3", "GET DATA",
    };

    // Oberthur ID One 128 v5.5 Dual CAC
    public String[] Oberthur_ID_One_128_v55_Dual_CAC_APDU_table = {
            "00a4040007a0000000790201", "SELECT GC1 Applet",
            "00a4040007a0000000790202", "SELECT GC2 Applet",
            "00a4040007a0000000030000", "SELECT CM",
            "80ca9f7f", "GET DATA",
    };

    // Oberthur ID One v5.2a Dual CAC
    public String[] Oberthur_ID_One_v52a_Dual_CAC_APDU_table = {
            "00a4040007a0000000790201", "SELECT GC1 Applet",
            "00a4040007a0000000790202", "SELECT GC2 Applet",
            "00a4040007a0000000030000", "SELECT CM",
            "80ca9f7f", "GET DATA",
    };

    // Gemalto GemXpresso 64K
    public String[] Gemalto_GemXpresso_64K_APDU_table = {
            "00a4040007a000000018434d", "SELECT CM",
            "80ca9f7f2d", "GET DATA",
    };

    // Pakistan eID
    public String[] Pakistan_eID_APDU_table = {
            "00A404000EA0000005176964656E7469747901", "SELECT eID Applet",
            "EE490000", "READ Citizen ID",
    };

    public String[] UAE_eID_APDU_table = {
            "00a404000CA00000024300130000000101", "SELECT eID Applet",
            "EE490000", "READ Citizen ID",
    };

    //US Passport
    public String[] US_ePassport_APDU_table = {
            "00A4040C07A0000002471001", "Select eID Applet",
            "0084000008", "GET DATA",
    };

    //MIFARE
    public String[] MiFARE_APDU_table = {
            "FFB00000000800", "Read 2K ",
            "FFB00000001000", "Read 4K",
    };

    public String[] APDU_table = noAPDU_table;

    public CardReaderPage(Context context) {
        super(context);
        initialize();
    }

    public CardReaderPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CardReaderPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        Log.d(TAG, "initialize");
        // Set proper xml page for this java file
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(R.layout.page_card_reader, this, true);
        // Initialize widgets
        status_text_view = (TextView) findViewById(R.id.textView1);
        button_view = (Button) findViewById(R.id.button1);
        button_open_close = (Button) findViewById(R.id.open_close);
        button_view.setText("Insert Card To Continue");
        // Close card for new page
        cardClosed();
        // populate the table with the cards we know about
        // Gemalto MPCOS EMV 8K
        new CardInfo("3b2a008065a20101014072d643",
                "Gemalto MPCOS EMV",
                MPCOS_EMV_APDU_table);
        // Gemalto Payflex 1K
        new CardInfo("3b6900002494010201000101a9",
                "Gemalto Payflex 1K",
                Payflex_1K_APDU_table);
        // G&D Sm@rtCafe Expert 64
        new CardInfo("3bfd1800ff80b1fe451f078073002113574a5448613147005f",
                "G & D SmartCafe Expert 64",
                GD_SmartCafe_Expert64_APDU_table);
        // Oberthur ID One 128 v5.5 Dual CAC
        new CardInfo("3bdb9600801f030031c064b0f3100007900080",
                "Oberthur 128 v5.5 D CAC",
                Oberthur_ID_One_128_v55_Dual_CAC_APDU_table);
        // Oberthur ID One v5.2a Dual CAC
        new CardInfo("3bdb9600801f030031c06477e30300829000c1",
                "Oberthur v5.2a D CAC",
                Oberthur_ID_One_v52a_Dual_CAC_APDU_table);
        // Gemalto GemXpresso 64K
        new CardInfo("3b6d000080318065b08302047e83009000",
                "Gemalto GemXpresso 64K",
                Gemalto_GemXpresso_64K_APDU_table);
        // Pakistan eID
        new CardInfo("3bdb960080b1fe451f830031c064b0fc100007900005",
                "Pakistan eID",
                Pakistan_eID_APDU_table);
        // UAE eID
        new CardInfo("3b7a9500008065a20131013d72d641",
                "UAE eID",
                UAE_eID_APDU_table);
        //US Passport
        //new CardInfo("3b8980018091e165d0004600000b", "US Passport", US_ePassport_APDU_table);
        new CardInfo("3b88800131cccc017781c1000e", "US Passport", US_ePassport_APDU_table);
        new CardInfo("3b8f8001804f0ca0000003060300020000000069", "MIfare 4K", MiFARE_APDU_table);
    }

    @Override
    public void activate(Biometrics biometrics) {
        // Initialize biometrics object
        mBiometrics = biometrics;
        // Make API call
        mBiometrics.registerCardStatusListener(new OnCardStatusListener() {
            @Override
            public void onCardStatusChange(String ATR, int prevState, int currentState) {
                // Log output debugging
                Log.d(TAG, "Card status changed !");
                Log.d(TAG, "ATR: [" + ATR + "] prevState: " + prevState + " currentState: " + currentState);
                // Reset apdu table to use
                APDU_table = noAPDU_table;
                APDU_state = APDU_INIT;
                APDU_idx = 0;
                // If card is not inserted/touched
                if (currentState == 1) {
                    // update user on whats going on
                    status_text_view.setText("Card Removed.\nPrevious State:" + prevState + "\nCurrent State:" + currentState);
                    button_view.setText("Insert Card To Continue");
                } else if (currentState == 2 || currentState == 4 || currentState == 5 || currentState == 6) {
                    // Initialize variables with intial valus
                    CardInfo ci;
                    String cardname = "{unknown}";
                    // Check if card is valid for apdu
                    if ((ci = (CardInfo) cardList.get(ATR)) != null) {
                        // Get card name
                        cardname = ci.getCardName();
                        // Set card apdu table
                        APDU_table = ci.getAPDU_table();
                    }
                    mCardName = cardname;
                    // Set text view for user to see card info
                    status_text_view.setText("Card Inserted" +
                            "\nPrevious State:" + prevState +
                            "\nCurrent State:" + currentState +
                            "\nCard:" + cardname +
                            "\nATR:\n" + ATR);
                    button_view.setText("APDU [" + APDU_table[APDU_idx + 1] + "]");
                } else if (prevState == 0 && currentState == 0 && ATR.equals("{ATR-null}")) {
                    //this combination is an indicator that the reader has been disconnected due to inactivity
                    status_text_view.setText("SmartCard reader disconnected");
                    button_view.setText("Open reader To Continue");
                }
            }
        });

        button_open_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button_open_close.getText().toString().equalsIgnoreCase(getResources().getString(R.string.open))) {
                    openClose(true);
                } else {
                    openClose(false);
                }
                button_open_close.setEnabled(false);
            }
        });
        button_view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                click();
            }
        });
    }

    protected void openClose(boolean open) {
        // If we are opening the card
        if (open) {
            // Set text view letting user know we are opening the card view
            status_text_view.setText("Opening Card Reader");
            open_cmd_counter = 0;
            // Make API call to open card
            mBiometrics.cardOpenCommand(new CardReaderStatusListner() {
                @Override
                public void onCardReaderOpen(ResultCode resultCode) {
                    // Increase counter of how many times card has been opened
                    open_cmd_counter++;
                    // Log output for debugging
                    Log.d(TAG, "SmartCard reader opened-" + open_cmd_counter + ", " + resultCode.name());
                    // Set text view letting user know results from open
                    status_text_view.setText("SmartCard reader RESULT:" + resultCode.toString());
                    // Set certain widgets on/off based on if result was good
                    cardOpened(resultCode == ResultCode.OK);
                }

                @Override
                public void onCardReaderClosed(CloseReasonCode reasonCode) {
                    // Increase counter of how many times card has been closed
                    close_cmd_counter++;
                    // Log output for debugging
                    Log.d(TAG, "SmartCard reader closed-" + close_cmd_counter);
                    // Set text view letting user know results from close
                    status_text_view.setText("SmartCard reader closed:" + reasonCode.toString());
                    // Turn on/off certain widgets
                    cardClosed();
                }
            });
        } else {
            // If we are turning off the card reader
            status_text_view.setText("Closing Card Reader");
            close_cmd_counter = 0;
            mBiometrics.cardCloseCommand();
        }
    }

    private void cardOpened(boolean readCard) {
        button_open_close.setEnabled(true);
        button_open_close.setText(readCard ? R.string.close : R.string.open);
        button_view.setEnabled(readCard);
    }

    private void cardClosed() {
        button_open_close.setEnabled(true);
        button_open_close.setText(R.string.open);
        button_view.setEnabled(false);
    }

    long start_time = 0;

    protected void click() {
        Log.d(TAG, "Button clicked !");
// Reset apdu variables
        if (APDU_state == APDU_INIT) {
            APDU_state = APDU_PROCESS;
            APDU_idx = 0;
        }

        // only call the service to send a non-empty APDU
        if (APDU_table[APDU_idx].length() > 0) {
            /* This portion of code creates a new ApduCommand using a byte array. Takes a command from the Apdu tables
               and just converts it to a byte array.
            byte[] command = new byte[7];
            for (int i = 0; i < 7; i++) {
                command[i] = (byte) ((Character.digit(APDU_table[APDU_idx].charAt(i), 16) << 4) + Character.digit(APDU_table[APDU_idx].charAt(i + 1), 16));
            }
            ApduCommand temp = new ApduCommand(command);
            */

            Log.d(TAG, APDU_table[APDU_idx]);
            ApduCommand APDU = new ApduCommand(APDU_table[APDU_idx]);
            APDU_lastdesc = APDU_table[APDU_idx + 1];
            start_time = SystemClock.elapsedRealtime();
            // Make API call to read card with created Apdu command
            mBiometrics.cardCommand(APDU, true, new OnCardCommandListener() {
                @Override
                public void onCardCommandComplete(ResultCode arg0, byte sw1, byte sw2, byte[] data) {
                    // Clear variable and log output showing result
                    String ds = "";
                    Log.d(TAG, "Resultcode is : " + arg0.toString());
                    // If result returned good
                    if (arg0 == ResultCode.OK) {
                        if (data.length == 0) {
                            ds = "{no data}";
                        } else {
                            // If data available then create string from data
                            int di;
                            for (di = 0; di < data.length; di++)
                                ds = ds + String.format("%02X", (0x0ff) & data[di]);
                        }
                        // Calculate time taking for result to come back good
                        long duration = SystemClock.elapsedRealtime() - start_time;
                        // Log output for debugging
                        Log.i(TAG, "Capture Complete in " + duration + "msec");
                        Log.d(TAG, "Card: " + mCardName
                                + " APDU Result: SW1,SW2: "
                                + String.format("%02x", (0x0ff) & sw1)
                                + String.format("%02x", (0x0ff) & sw2)
                                + " D(" + data.length + "): " + ds);
                        // Set text view for user to see what is going on
                        status_text_view.setText("Card: " + mCardName + "\n\n"
                                + "Mili-Seconds elapsed:" + duration + "\n\n"
                                + "APDU [" + APDU_lastdesc + "]\n\n"
                                + "SW1,SW2: "
                                + String.format("%02x", (0x0ff) & sw1)
                                + String.format("%02x", (0x0ff) & sw2) + "\n\n"
                                + "Data:\n" + ds + "\n\n" + "Length: "
                                + data.length + " bytes");
                    } else {
                        // If result did failed reset all variables and let user know failure
                        APDU_state = APDU_INIT;
                        APDU_idx = 0;
                        status_text_view.setText("Error, Please Try Again");
                    }
                }
            });
        }

        APDU_idx += 2; // skip descriptive string

        // reset APDU table index to top of table if done
        if (APDU_idx >= APDU_table.length) {
            Log.d(TAG, "Resetting APDU_idx to start of APDU_table");
            APDU_state = APDU_INIT;
            APDU_idx = 0;
        }

        button_view.setText("APDU [" + APDU_table[APDU_idx + 1] + "]");
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.title_card_reader);
    }

    @Override
    public void doResume() {
        Log.d(TAG, "doResume ");
//		if ( mBiometrics != null )
//			mBiometrics.cardOpenCommand(new CardReaderStatusListner(){
//				@Override
//				public void onCardReaderOpen() {
//					status_text_view.setText("onCardReaderOpen called");
//					cardOpened();
//				}
//
//				@Override
//				public void onCardReaderClosed() {
//					Log.d(TAG, "SmartCard reader closed  !");
//					status_text_view.setText("SmartCard reader closed");
//					cardClosed();	
//				}
//			});
    }

    @Override
    public void deactivate() {

    }

    class CardInfo {
        // Holds card name, apdu table, and atr value
        private String mATR;
        private String mCardName;
        private String[] mAPDU_table;

        public CardInfo(String ATR, String CardName, String[] APDU_table) {
            this.mATR = ATR;
            this.mCardName = CardName;
            this.mAPDU_table = APDU_table;
            // Add card atr to list
            cardList.put(mATR, this);
        }

        public String getATR() {
            return (mATR);
        }

        public String getCardName() {
            return (mCardName);
        }

        public String[] getAPDU_table() {
            return (mAPDU_table);
        }
    }
}
