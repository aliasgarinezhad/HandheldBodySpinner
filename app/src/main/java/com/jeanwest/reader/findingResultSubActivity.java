package com.jeanwest.reader;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.rscja.deviceapi.RFIDWithUHF;
import com.rscja.deviceapi.exception.ConfigurationException;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class findingResultSubActivity extends AppCompatActivity {

    public static RFIDWithUHF RF;
    public static Map<String, Integer> EPCTableFinding = new HashMap<>();
    ToneGenerator beep = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    private int findingPower = 30;
    TextView status;
    TextView powerText;
    SeekBar powerSeekBar;
    TextView numberOfFoundText;
    TextView stuffSpec;
    findingSubThread findTask;
    private long stuffCode;
    WebView picture;
    CheckBox option;

    public static String stuffPrimaryCode;
    public static String stuffRFIDCode;
    JSONObject stuff;
    Map<String, Integer> EPCTableFindingMatched = new HashMap<>();
    WebSettings setting;

    boolean isChecked = false;
    boolean findingInProgress = false;
    public static boolean isProcessing = false;

    Handler databaseBackgroundTaskHandler = new Handler();

    Runnable databaseBackgroundTask = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {

            if(findingInProgress) {

                isProcessing = true;

                if(EPCTableFinding.size() > 0) {

                    flag = false;

                    for (Map.Entry<String, Integer> EPC : EPCTableFinding.entrySet()) {

                        EPCHexString = EPC.getKey();
                        EPCInt1 = Long.parseLong(EPCHexString.substring(0, 8), 16);
                        EPCInt2 = Long.parseLong(EPCHexString.substring(8, 16), 16);
                        EPCInt3 = Long.parseLong(EPCHexString.substring(16, 24), 16);

                        EPCBinaryString1 = Long.toBinaryString(EPCInt1);
                        EPCBinaryString1 = String.format("%32s", EPCBinaryString1).replaceAll(" ", "0");
                        EPCBinaryString2 = Long.toBinaryString(EPCInt2);
                        EPCBinaryString2 = String.format("%32s", EPCBinaryString2).replaceAll(" ", "0");
                        EPCBinaryString3 = Long.toBinaryString(EPCInt3);
                        EPCBinaryString3 = String.format("%32s", EPCBinaryString3).replaceAll(" ", "0");

                        EPCBinaryString = EPCBinaryString1 + EPCBinaryString2 + EPCBinaryString3;

                        header = Integer.parseInt(EPCBinaryString.substring(0, 8), 2);
                        companyNumber = Integer.parseInt(EPCBinaryString.substring(14, 26), 2);
                        itemNumber = Long.parseLong(EPCBinaryString.substring(26, 58), 2);

                        if (companyNumber == 100) {
                            stuffCode = Long.parseLong(stuffPrimaryCode);
                        } else if (companyNumber == 101) {
                            stuffCode = Long.parseLong(stuffRFIDCode);
                        }
                        if (header == 48 && itemNumber == stuffCode) {
                            //status.setText(status.getText() + "\n" + EPCHexString);
                            EPCTableFindingMatched.put(EPCHexString, 1);
                            flag = true;
                        }
                    }
                    isProcessing = false;

                    numberOfFoundText.setText(String.valueOf(EPCTableFindingMatched.size()));
                    status.setText("");
                    status.setText("در حال جست و جو ...");

                    for(Map.Entry<String, Integer> valid : EPCTableFindingMatched.entrySet()) {
                        status.setText(status.getText() + "\n" + valid.getKey());
                    }

                    if (!flag) {
                        //beep.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
                    } else {
                        beep.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
                    }

                    EPCTableFinding.clear();
                    databaseBackgroundTaskHandler.postDelayed(databaseBackgroundTask, 1000);
                }
                else {
                    isProcessing = false;
                    //numberOfFoundText.setText(String.valueOf(EPCTableFindingMatched.size()));
                    //beep.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
                    databaseBackgroundTaskHandler.postDelayed(databaseBackgroundTask, 1000);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding_result_sub);
        status = findViewById(R.id.section_label0);
        stuffSpec = findViewById(R.id.result0);
        picture = findViewById(R.id.pictureView0);
        powerText = findViewById(R.id.findingPowerTextView0);
        powerSeekBar = findViewById(R.id.findingPowerSeekBar0);
        numberOfFoundText = findViewById(R.id.numberOfFoundTextView0);
        option = findViewById(R.id.checkBox0);

        powerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (!findTask.readEnable) {
                    findingPower = progress + 5;
                    powerText.setText("قدرت سیگنال(" + findingPower + ")");
                } else {
                    RF.stopInventory();
                    findingPower = progress + 5;
                    powerText.setText("قدرت سیگنال(" + findingPower + ")");
                    //powerSeekBar.setProgress(findingPower - 5);
                    while (!RF.setPower(findingPower)) {}
                    RF.startInventoryTag(0,0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        try {
            RF = RFIDWithUHF.getInstance();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();

        findTask = new findingSubThread();
        findTask.stop = false;
        findTask.start();

        while (!RF.setPower(findingPower)) {
        }

        try {

            stuff = findingActivity.API.similar.getJSONObject(findingActivity.index);
            stuffSpec.setText(stuff.getString("productName") + "\n" +
                    "کد محصول: " + stuff.getString("K_Bar_Code") + "\n" +
                    "بارکد: " + stuff.getString("KBarCode") + "\n" +
                    "قیمت مصرف کننده: " + stuff.getString("OrigPrice") + "\n" +
                    "قیمت فروش: " + stuff.getString("SalePrice") + "\n" +
                    "موجودی فروشگاه: " + stuff.getString("dbCountStore") + "\n" +
                    "موجودی انبار: " + stuff.getString("dbCountDepo"));

            picture.loadUrl(stuff.getString("ImgUrl"));
            stuffPrimaryCode = stuff.getString("BarcodeMain_ID");
            stuffRFIDCode = stuff.getString("RFID");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setting = picture.getSettings();
        setting.setUseWideViewPort(true);
        setting.setLoadWithOverviewMode(true);
        picture.setFocusable(false);

        reading.databaseInProgress = false;
        powerSeekBar.setProgress(findingPower - 5);
        powerText.setText("قدرت سیگنال(" + findingPower + ")");

    }

    int header;
    int companyNumber;
    long itemNumber;
    boolean flag;
    long EPCInt1;
    long EPCInt2;
    long EPCInt3;
    String EPCHexString;
    String EPCBinaryString;
    String EPCBinaryString1;
    String EPCBinaryString2;
    String EPCBinaryString3;

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == 280 || keyCode == 139) {

            if (event.getRepeatCount() == 0) {

                status.setText("");

                if (!findTask.readEnable) {

                    EPCTableFinding.clear();
                    if(!isChecked) {
                        EPCTableFindingMatched.clear();
                    }

                    while (!RF.setPower(findingPower)) {
                    }
                    findTask.readEnable = true;

                    status.setText("در حال جست و جو ...");
                    findingInProgress = true;
                    databaseBackgroundTaskHandler.postDelayed(databaseBackgroundTask, 1000);
                }
                else {

                    databaseBackgroundTaskHandler.removeCallbacks(databaseBackgroundTask);

                    findTask.readEnable = false;
                    while (!findTask.finished) {
                    }

                    findingInProgress = false;

                    flag = false;

                    for (Map.Entry<String, Integer> EPC : EPCTableFinding.entrySet()) {

                        EPCHexString = EPC.getKey();
                        EPCInt1 = Long.parseLong(EPCHexString.substring(0, 8), 16);
                        EPCInt2 = Long.parseLong(EPCHexString.substring(8, 16), 16);
                        EPCInt3 = Long.parseLong(EPCHexString.substring(16, 24), 16);

                        EPCBinaryString1 = Long.toBinaryString(EPCInt1);
                        EPCBinaryString1 = String.format("%32s", EPCBinaryString1).replaceAll(" ", "0");
                        EPCBinaryString2 = Long.toBinaryString(EPCInt2);
                        EPCBinaryString2 = String.format("%32s", EPCBinaryString2).replaceAll(" ", "0");
                        EPCBinaryString3 = Long.toBinaryString(EPCInt3);
                        EPCBinaryString3 = String.format("%32s", EPCBinaryString3).replaceAll(" ", "0");

                        EPCBinaryString = EPCBinaryString1 + EPCBinaryString2 + EPCBinaryString3;

                        header = Integer.parseInt(EPCBinaryString.substring(0, 8), 2);
                        companyNumber = Integer.parseInt(EPCBinaryString.substring(14, 26), 2);
                        itemNumber = Long.parseLong(EPCBinaryString.substring(26, 58), 2);

                        if (companyNumber == 100) {
                            stuffCode = Long.parseLong(stuffPrimaryCode);
                        } else if (companyNumber == 101) {
                            stuffCode = Long.parseLong(stuffRFIDCode);
                        }
                        if (header == 48 && itemNumber == stuffCode) {

                            EPCTableFindingMatched.put(EPCHexString, 1);
                            flag = true;
                        }
                    }
                    numberOfFoundText.setText(String.valueOf(EPCTableFindingMatched.size()));

                    status.setText("");
                    for(Map.Entry<String, Integer> valid : EPCTableFindingMatched.entrySet()) {
                        status.setText(status.getText() + "\n" + valid.getKey());
                    }

                    if (!flag) {
                        //beep.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
                    } else {
                        beep.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
                    }

                    EPCTableFinding.clear();
                }
            }

        } else if (keyCode == 4) {

            if(findTask.readEnable) {
                RF.stopInventory();
                findTask.readEnable = false;
            }

            findingInProgress = false;
            databaseBackgroundTaskHandler.removeCallbacks(databaseBackgroundTask);

            findTask.stop = true;
            finish();
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(findTask.readEnable) {
            RF.stopInventory();
            findTask.readEnable = false;
        }
    }

    public void clearEPCs(View view) {

        EPCTableFinding.clear();
        EPCTableFindingMatched.clear();
        status.setText("");
    }

    public void optionChange(View view) {

        isChecked = option.isChecked();
    }
}