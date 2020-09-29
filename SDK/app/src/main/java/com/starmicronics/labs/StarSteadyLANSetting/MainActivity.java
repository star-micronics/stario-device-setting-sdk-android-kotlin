package com.starmicronics.labs.StarSteadyLANSetting;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.app.AlertDialog;

import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;


import com.starmicronics.labs.StarSteadyLANSetting.Communication.CommunicationResult;
import com.starmicronics.labs.StarSteadyLANSetting.Communication.Result;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String IF_TYPE_ETHERNET = "TCP:";
    private static final String IF_TYPE_BLUETOOTH = "BT:";
    private static final String IF_TYPE_USB = "USB:";

    private ArrayAdapter<ItemList> adapter;
    private String    mPortName;
    private String    mModelName;
    private String    mMacAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<ItemList> list = new ArrayList<>();
        adapter = new ItemListAdapter(this, list);
        ListView mListView = findViewById(R.id.searchPrinterList_listView);
        TextView emptyTextView = findViewById(R.id.emptyTextView);
        mListView.setEmptyView(emptyTextView);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);


        final Button applyButton = findViewById(R.id.apply_button);
        applyButton.setOnClickListener(this);

        final Button readSettingButton = findViewById(R.id.readSetting_button);
        readSettingButton.setOnClickListener(this);

        final Button searchPrinterButton = findViewById(R.id.searchPrinter_button);
        searchPrinterButton.setOnClickListener(this);
    }


    public void onClick(View view){

        if(view.getId() == R.id.apply_button){

            Log.i ("LOG", "Apply RemoteConfig Setting");

            byte[] commands;
            Spinner spinner = findViewById(R.id.steadyLANSetting_spinner);
            if (spinner.getSelectedItemPosition() == 1 )
            {
                commands = new byte[]{ 0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x39, 0x01, 0x02,  //set to SteadyLAN(for Android).
                                       0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x70, 0x01, 0x00}; //apply setting. Note: The printer is reset to apply setting when writing this command is completed.

                //The settings for other OSs are as follows. But it will not work on Android devices.
                // For iOS
            //  commands = new byte[]{ 0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x39, 0x01, 0x01,  //set to SteadyLAN(for iOS).
            //                         0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x70, 0x01, 0x00}; //apply setting. Note: The printer is reset to apply setting when writing this command is completed.

                // For Windows
            //  commands = new byte[]{ 0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x39, 0x01, 0x03,  //set to SteadyLAN(for Windows).
            //                         0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x70, 0x01, 0x00}; //apply setting. Note: The printer is reset to apply setting when writing this command is completed.

            }
            else
            {
                commands = new byte[]{ 0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x39, 0x01, 0x00,  //set to SteadyLAN(Disable)
                                       0x1b, 0x1d, 0x29, 0x4e, 0x03, 0x00, 0x70, 0x01, 0x00}; //apply setting. Note: The printer is reset to apply setting when writing this command is completed.
            }

            Communication.sendCommands(this, commands, mPortName, "", 10000, this, mCallback);

        }
        else if(view.getId() == R.id.readSetting_button){

            Log.i ("LOG", "Read SteadyLAN Setting");
            Communication.confirmSteadyLANSetting(this, mPortName, "", 10000, this, mSteadyLANSettingCallback);


        }
        else if(view.getId() == R.id.searchPrinter_button)
        {
            Log.i ("LOG", "Search Star Printer");

            adapter.clear();
            String[] interfaceTypes = {IF_TYPE_ETHERNET, IF_TYPE_BLUETOOTH, IF_TYPE_USB};

            for (String interfaceType : interfaceTypes) {
                SearchTask searchTask = new SearchTask();
                searchTask.execute(interfaceType);
            }
        }
    }


    private class SearchTask extends AsyncTask<String, Void, Void> {
        private ArrayList<com.starmicronics.stario.PortInfo> mPortList;

        SearchTask() {
            super();
        }

        @Override
        protected Void doInBackground(String... interfaceType) {
            try {
                mPortList = StarIOPort.searchPrinter(interfaceType[0], getApplicationContext());
            }
            catch (StarIOPortException e) {
                mPortList = new ArrayList<>();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void doNotUse) {
            for (PortInfo info : mPortList) {
                addItem(info);
            }

        }
    }


    private void addItem(PortInfo info) {
        List<TextInfo> textList = new ArrayList<>();
        List<ImgInfo>  imgList  = new ArrayList<>();

        String modelName;
        String portName;
        String macAddress;

        // --- Bluetooth ---
        // It can communication used device name(Ex.BT:Star Micronics) at bluetooth.
        // If android device has paired two same name device, can't choose destination target.
        // If used Mac Address(Ex. BT:00:12:3f:XX:XX:XX) at Bluetooth, can choose destination target.
        if (info.getPortName().startsWith(IF_TYPE_BLUETOOTH)) {
            modelName  = info.getPortName().substring(IF_TYPE_BLUETOOTH.length());
            portName   = IF_TYPE_BLUETOOTH + info.getMacAddress();
            macAddress = info.getMacAddress();
        }
        else {
            modelName  = info.getModelName();
            portName   = info.getPortName();
            macAddress = info.getMacAddress();
        }

        textList.add(new TextInfo(modelName,  R.id.modelNameTextView));
        textList.add(new TextInfo(portName,   R.id.portNameTextView));

        if ( info.getPortName().startsWith(IF_TYPE_ETHERNET) || info.getPortName().startsWith(IF_TYPE_BLUETOOTH)) {
            textList.add(new TextInfo("(" + macAddress + ")", R.id.macAddressTextView));
        }

        imgList.add(new ImgInfo(R.drawable.unchecked_icon, R.id.checkedIconImageView));

        adapter.add(new ItemList(R.layout.list_printer_info_row, textList, imgList));

    }

    private final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(CommunicationResult communicationResult) {

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Communication Result")
                    .setMessage(Communication.getCommunicationResultMessage(communicationResult))
                    .setPositiveButton("OK", null)
                    .show();
        }
    };


    private final Communication.SteadyLANSettingCallback mSteadyLANSettingCallback = new Communication.SteadyLANSettingCallback() {
        @Override
        public void onRemoteConfigSetting(CommunicationResult communicationResult, String steadyLANSetting) {

            String dialogMessage = "";

            if (communicationResult.getResult() == Result.Success) {
                dialogMessage = steadyLANSetting;
            }
            else {
                dialogMessage = Communication.getCommunicationResultMessage(communicationResult);
            }

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("SteadyLAN Setting")
                    .setMessage(dialogMessage)
                    .setPositiveButton("OK", null)
                    .show();
        }
    };


    public void onItemClick(AdapterView<?> parent, View clickedItemView, int position, long id) {

        switchSelectedRow(position);
        mMacAddress = "";

        List<TextInfo> portInfoList = adapter.getItem(position).getTextList();

        for (TextInfo portInfo : portInfoList) {
            switch (portInfo.getTextResourceID()) {
                case R.id.modelNameTextView:
                    mModelName = portInfo.getText();
                    break;
                case R.id.portNameTextView:
                    mPortName = portInfo.getText();
                    break;
                case R.id.macAddressTextView:
                    mMacAddress = portInfo.getText();
                    if (mMacAddress.startsWith("(") && mMacAddress.endsWith(")")) {
                        mMacAddress = mMacAddress.substring(1, mMacAddress.length() - 1);
                    }
                    break;
            }
        }

    }

    private void switchSelectedRow(int index) {
        for (int i = 0; i < adapter.getCount(); i++) {
            ItemList itemList = adapter.getItem(i);

            if (itemList.getImgList() == null) {
                continue;
            }

            List<ImgInfo> imgList = new ArrayList<>();

            int imageId;

            if (i == index) {
                imageId = R.drawable.checked_icon;
            }
            else {
                imageId = R.drawable.unchecked_icon;
            }

            imgList.add(new ImgInfo(imageId, R.id.checkedIconImageView));

            itemList.setImgList(imgList);

            adapter.remove(itemList);
            adapter.insert(itemList, i);
        }
    }

}



