package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.ApduCommand;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CardReaderStatusListner;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.OnCardCommandListener;
import com.credenceid.biometrics.Biometrics.OnCardStatusListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.TheApp;
import com.credenceid.sdkapp.models.ApduState;
import com.credenceid.sdkapp.models.CardInfo;
import com.credenceid.sdkapp.models.PageView;

import java.util.Hashtable;

import static com.credenceid.sdkapp.models.CardDataTables.GD_SmartCafe_Expert64_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.Gemalto_GemXpresso_64K_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.MPCOS_EMV_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.MiFARE_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.Oberthur_ID_One_128_v55_Dual_CAC_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.Oberthur_ID_One_v52a_Dual_CAC_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.Pakistan_eID_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.Payflex_1K_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.UAE_eID_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.US_ePassport_APDU_table;
import static com.credenceid.sdkapp.models.CardDataTables.noAPDU_table;

public class CardReaderPage extends LinearLayout implements PageView {
    private static final String TAG = CardReaderPage.class.getName();
    /* Keeps track of how many times CardReader was opened/closed. */
    private static int closeCommandCount = 0;
    private static int openCommandCount = 0;
    /* Hashtable to store all of our different known card types, CardInfo objects. */
    private static Hashtable cardTable = new Hashtable();
    public String APDU_lastdesc = "{unknown}";
    public String cardName = "{unknown}";
    public String[] APDU_table = noAPDU_table;
    /* Current index in Hashtable for which we are referring to. */
    int apduTableIndex = 0;
    /* Holds current APDU command state of CardReader. */
    ApduState apduState = ApduState.APDU_INIT;
    /* Layout components. */
    TextView textViewStatus;
    Button buttonView;
    Button buttonOpenClose;
    Button buttonSyncAssync;
    private boolean syncMode;
    private int counter = 0;

    /* Our custom listener to be invoked every time CardReader status changes. */
    OnCardStatusListener onCardStatusListener = new OnCardStatusListener() {
        @Override
        public void onCardStatusChange(String ATR, int prevState, int currentState) {
            Log.d(TAG, "Card status changed !");
            Log.d(TAG, "ATR: [" + ATR + "] prevState: " + prevState + " currentState: " + currentState);

            /* Every time status changes we need to reset our tables since we do not know what next
             * card type could be.
             */
            APDU_table = noAPDU_table;
            apduState = ApduState.APDU_INIT;
            apduTableIndex = 0;

            /* If there is no card present. */
            if (currentState == 1) {
                textViewStatus.setText("Card Removed.\nPrevious State:" + prevState + "\nCurrent State:" + currentState);
                buttonView.setText(R.string.insert_card);
            } else if (currentState >= 2 && currentState <= 6) {
                /* Get respective CardInformation by ATR. */
                CardInfo ci = (CardInfo) cardTable.get(ATR);
                /* If it is a valid card then set our local cardname and the APDU table to use. */
                String localCardName = (ci != null) ? ci.getCardName() : "{unknown}";
                APDU_table = (ci != null) ? ci.getApduTable() : APDU_table;
                /* Set global cardName variable to match local's. */
                cardName = localCardName;

                /* Set text view for user to see card info. */
                textViewStatus.setText("Card Inserted" +
                        "\nPrevious State:" + prevState +
                        "\nCurrent State:" + currentState +
                        "\nCard:" + localCardName +
                        "\nATR:\n" + ATR);

                buttonView.setText("APDU [" + APDU_table[apduTableIndex + 1] + "]");
            } else if (prevState == 0 && currentState == 0 && ATR.equals("{ATR-null}")) {
                /* This combination is an indicator that the reader has been disconnected due
                 * to inactivity.
                 */
                textViewStatus.setText(R.string.card_reader_disconnected);
                buttonView.setText(R.string.open_card_reader);
            }
        }
    };

    /* Biometrics object for making Credence API calls. */
    private Biometrics biometrics;

    /* Alternatively BiometricsManager object for making Credence API calls. */
    private BiometricsManager biometricsManager;

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
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(R.layout.page_card_reader, this, true);

        this.initializeLayoutComponents();
        this.populateCardTable();

        biometricsManager = TheApp.getInstance().getBiometricsManager();
    }

    private void initializeLayoutComponents() {
        textViewStatus = (TextView) findViewById(R.id.status_tv);

        buttonSyncAssync = (Button) findViewById(R.id.sync_button);
        buttonSyncAssync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (counter % 2 == 0) {
                    buttonSyncAssync.setText("Sync");
                    Toast.makeText(getContext(), "Synchronous mode selected", Toast.LENGTH_SHORT).show();
                    syncMode = true;
                    counter++;
                } else {
                    buttonSyncAssync.setText("Async");
                    Toast.makeText(getContext(), "Asynchronous mode selected", Toast.LENGTH_SHORT).show();
                    syncMode = false;
                    counter++;
                }
            }
        });

        buttonOpenClose = (Button) findViewById(R.id.open_close);
        buttonOpenClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonOpenClose.getText().toString().equalsIgnoreCase(getResources().getString(R.string.open)))
                    openCardReader();
                else closeCardReader();
                buttonOpenClose.setEnabled(false);
            }
        });
        buttonOpenClose.setEnabled(true);
        buttonOpenClose.setText(R.string.open);

        buttonView = (Button) findViewById(R.id.view);
        buttonView.setText("Insert Card To Continue");
        buttonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                click();
            }
        });
        buttonView.setEnabled(false);
    }

    private void populateCardTable() {
        new CardInfo("3b2a008065a20101014072d643",
                "Gemalto MPCOS EMV",
                MPCOS_EMV_APDU_table,
                cardTable);
        new CardInfo("3b6900002494010201000101a9",
                "Gemalto Payflex 1K",
                Payflex_1K_APDU_table,
                cardTable);
        new CardInfo("3bfd1800ff80b1fe451f078073002113574a5448613147005f",
                "G & D SmartCafe Expert 64",
                GD_SmartCafe_Expert64_APDU_table,
                cardTable);
        new CardInfo("3bdb9600801f030031c064b0f3100007900080",
                "Oberthur 128 v5.5 D CAC",
                Oberthur_ID_One_128_v55_Dual_CAC_APDU_table,
                cardTable);
        new CardInfo("3bdb9600801f030031c06477e30300829000c1",
                "Oberthur v5.2a D CAC",
                Oberthur_ID_One_v52a_Dual_CAC_APDU_table,
                cardTable);
        new CardInfo("3b6d000080318065b08302047e83009000",
                "Gemalto GemXpresso 64K",
                Gemalto_GemXpresso_64K_APDU_table,
                cardTable);
        new CardInfo("3bdb960080b1fe451f830031c064b0fc100007900005",
                "Pakistan eID",
                Pakistan_eID_APDU_table,
                cardTable);
        new CardInfo("3b7a9500008065a20131013d72d641",
                "UAE eID",
                UAE_eID_APDU_table,
                cardTable);
        new CardInfo("3bf99600008131fe454a434f50323432523327",
                "US Passport",
                US_ePassport_APDU_table,
                cardTable);
        new CardInfo("3b8f8001804f0ca0000003060300020000000069",
                "MIfare 4K",
                MiFARE_APDU_table,
                cardTable);
    }

    @Override
    public void activate(Biometrics biometrics) {
        this.biometrics = biometrics;
        /* Every time we activate this page we need to register the card status change listener,
         * since we unregister it during deactivate().
         */
        this.biometrics.registerCardStatusListener(this.onCardStatusListener);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.title_card_reader);
    }

    @Override
    public void doResume() {
        Log.d(TAG, "doResume ");
    }

    @Override
    public void deactivate() {
        Log.d(TAG, "deactivate ");
    }

    /* Based on parameter will either call Open Card reader API or close API. */
    protected void openCardReader() {
        // Set text view letting user know we are opening the card view
        textViewStatus.setText(R.string.opening_card_reader);
        openCommandCount = 0;

        this.biometrics.cardOpenCommand(new CardReaderStatusListner() {
            @Override
            public void onCardReaderOpen(ResultCode resultCode) {
                /* Increase counter of how many times card has been opened. */
                ++openCommandCount;
                Log.d(TAG, "SmartCard reader opened-" + openCommandCount + ", " + resultCode.name());

                textViewStatus.setText(R.string.reader_result + resultCode.toString());
                /* Set certain widgets on/off based on if result was good. */
                cardOpened(resultCode == ResultCode.OK);
            }

            @Override
            public void onCardReaderClosed(ResultCode resultCode, CloseReasonCode reasonCode) {
                if (resultCode == ResultCode.OK) {
                    /* Increase counter of how many times card has been closed. */
                    closeCommandCount++;
                    Log.d(TAG, "SmartCard reader closed-" + closeCommandCount);

                    textViewStatus.setText("SmartCard reader closed:" + reasonCode.toString());
                    /* Turn on/off certain widgets.*/
                    cardClosed();
                } else if (resultCode == ResultCode.FAIL) {
                    Log.d(TAG, "SmartCard reader closed: FAILED");
                    textViewStatus.setText("SmartCard reader closed: FAILED");
                    buttonOpenClose.setEnabled(true);
                }

            }
        });
    }

    private void closeCardReader() {
        textViewStatus.setText(R.string.closing_card_reader);
        closeCommandCount = 0;
        this.biometrics.cardCloseCommand();
    }

    /* Update buttons state based on parameter. */
    private void cardOpened(boolean readCard) {
        buttonOpenClose.setEnabled(true);
        buttonOpenClose.setText(readCard ? R.string.close : R.string.open);
        buttonView.setEnabled(readCard);
    }

    /* Update buttons to close state. */
    private void cardClosed() {
        buttonOpenClose.setEnabled(true);
        buttonOpenClose.setText(R.string.open);
        buttonView.setEnabled(false);
    }

    /* This method calls Card Command API to see data on card based on states. */
    protected void click() {
        Log.d(TAG, "Button clicked !");

        /* Reset apdu variables. */
        if (apduState == ApduState.APDU_INIT) {
            apduState = ApduState.APDU_PROCESS;
            apduTableIndex = 0;
        }

        /* Only call the service to send a non-empty APDU. */
        if (APDU_table[apduTableIndex].length() > 0) {
            /* This portion of code creates a new ApduCommand using a byte array. Takes a command
             * from the Apdu tables and just converts it to a byte array.
             */
            /*
            byte[] command = new byte[7];
            for (int i = 0; i < 7; i++) {
                command[i] =
                        (byte) ((Character.digit(APDU_table[apduTableIndex].charAt(i), 16) << 4) +
                                Character.digit(APDU_table[apduTableIndex].charAt(i + 1), 16));
            }
            ApduCommand temp = new ApduCommand(command);
            */

            Log.d(TAG, APDU_table[apduTableIndex]);
            ApduCommand APDU = new ApduCommand(APDU_table[apduTableIndex]);
            APDU_lastdesc = APDU_table[apduTableIndex + 1];

            /* Keep track of how long API call takes. */
            final long startTime = SystemClock.elapsedRealtime();

            if (!syncMode) {
                biometricsManager.cardCommand(APDU, true, new OnCardCommandListener() {
                    @Override
                    public void onCardCommandComplete(ResultCode arg0, byte sw1, byte sw2, byte[] data) {
                        String ds = "";
                        Log.d(TAG, "Resultcode is : " + arg0.toString());
                        Log.i(TAG, "Asynchronous mode");

                        if (arg0 == ResultCode.OK) {
                        /* Calculate time taking for result to come back good. */
                            long duration = SystemClock.elapsedRealtime() - startTime;

                            if (data.length == 0) ds = "{no data}";
                            else
                            /* If data available then create string from data. */
                                for (byte peice : data)
                                    ds = ds + String.format("%02X", (0x0ff) & peice);

                            Log.i(TAG, "Capture Complete in " + duration + "msec");
                            Log.d(TAG, "Card: " + cardName
                                    + " APDU Result: SW1,SW2: "
                                    + String.format("%02x", (0x0ff) & sw1)
                                    + String.format("%02x", (0x0ff) & sw2)
                                    + " D(" + data.length + "): " + ds);

                            textViewStatus.setText("Card: " + cardName + "\n\n"
                                    + "Mili-Seconds elapsed:" + duration + "\n\n"
                                    + "APDU [" + APDU_lastdesc + "]\n\n"
                                    + "SW1,SW2: "
                                    + String.format("%02x", (0x0ff) & sw1)
                                    + String.format("%02x", (0x0ff) & sw2) + "\n\n"
                                    + "Data:\n" + ds + "\n\n" + "Length: "
                                    + data.length + " bytes");
                        } else {
                        /* If result FAILED reset all variables and let user know failure. */
                            apduState = ApduState.APDU_INIT;
                            apduTableIndex = 0;
                            textViewStatus.setText("Error, Please Try Again");
                        }
                    }
                });

            } else {
                biometricsManager.cardCommandSync(APDU, true, new OnCardCommandListener() {
                    @Override
                    public void onCardCommandComplete(ResultCode arg0, byte sw1, byte sw2, byte[] data) {
                        String ds = "";
                        Log.d(TAG, "Resultcode is : " + arg0.toString());
                        Log.i(TAG, "Synchronous mode");

                        if (arg0 == ResultCode.OK) {
                        /* Calculate time taking for result to come back good. */
                            long duration = SystemClock.elapsedRealtime() - startTime;

                            if (data.length == 0) ds = "{no data}";
                            else
                            /* If data available then create string from data. */
                                for (byte peice : data)
                                    ds = ds + String.format("%02X", (0x0ff) & peice);

                            Log.i(TAG, "Capture Complete in " + duration + "msec");
                            Log.d(TAG, "Card: " + cardName
                                    + " APDU Result: SW1,SW2: "
                                    + String.format("%02x", (0x0ff) & sw1)
                                    + String.format("%02x", (0x0ff) & sw2)
                                    + " D(" + data.length + "): " + ds);

                            textViewStatus.setText("Card: " + cardName + "\n\n"
                                    + "Mili-Seconds elapsed:" + duration + "\n\n"
                                    + "APDU [" + APDU_lastdesc + "]\n\n"
                                    + "SW1,SW2: "
                                    + String.format("%02x", (0x0ff) & sw1)
                                    + String.format("%02x", (0x0ff) & sw2) + "\n\n"
                                    + "Data:\n" + ds + "\n\n" + "Length: "
                                    + data.length + " bytes");
                        } else {
                        /* If result FAILED reset all variables and let user know failure. */
                            apduState = ApduState.APDU_INIT;
                            apduTableIndex = 0;
                            textViewStatus.setText("Error, Please Try Again");
                        }
                    }

                }, 5000);

            }

        /* Skip descriptive string. */
            apduTableIndex += 2;

        /* Reset APDU table index to top of table if done. */
            if (apduTableIndex >= APDU_table.length) {
                Log.d(TAG, "Resetting apduTableIndex to start of APDU_table");
                apduState = ApduState.APDU_PROCESS;
                apduTableIndex = 0;
            }
            buttonView.setText("APDU [" + APDU_table[apduTableIndex + 1] + "]");
        }
    }
}