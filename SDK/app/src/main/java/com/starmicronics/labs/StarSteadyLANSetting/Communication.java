package com.starmicronics.labs.StarSteadyLANSetting;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.starmicronics.labs.StarSteadyLANSetting.Communication.CommunicationResult;
import com.starmicronics.labs.StarSteadyLANSetting.Communication.Result;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.stario.StarResultCode;

import java.util.ArrayList;
import java.util.List;


class SendCommandThread extends Thread {
    private final Object mLock;
    private Communication.SendCallback mCallback;
    private byte[] mCommands;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;


    SendCommandThread(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, Communication.SendCallback callback) {
        mLock         = lock;
        mCommands     = commands;
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
    }

    @Override
    public void run() {
        Result result = Result.ErrorOpenPort;
        int code = StarResultCode.FAILURE;

        synchronized (mLock) {
            try {
                if (mPort == null) {

                    if (mPortName == null) {
                        resultSendCallback(result, code, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }
                if (mPort == null) {
                    result = Result.ErrorOpenPort;
                    resultSendCallback(result, code, mCallback);
                    return;
                }

                StarPrinterStatus status;

                result = Result.ErrorWritePort;

                status = mPort.retreiveStatus();

                if (status.offline == true) {
                    throw new StarIOPortException("A printer is offline.");
                }

                result = Result.ErrorWritePort;

                mPort.writePort(mCommands, 0, mCommands.length);

                result = Result.Success;
                code = StarResultCode.SUCCESS;
            } catch (StarIOPortException e) {
                code = e.getErrorCode();
            }

            if (mPort != null && mPortName != null) {
                try {
                    StarIOPort.releasePort(mPort);
                } catch (StarIOPortException e) {
                    // Nothing
                }
                mPort = null;
            }

            resultSendCallback(result, code, mCallback);
        }
    }

    private static void resultSendCallback(final Result result, final int code, final Communication.SendCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(new CommunicationResult(result, code));
                }
            });
        }
    }
}

class SteadyLANSettingThread extends Thread {
    private final Object mLock;
    private Communication.SteadyLANSettingCallback mCallback;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    SteadyLANSettingThread(Object lock, String portName, String portSettings, int timeout, Context context, Communication.SteadyLANSettingCallback callback) {
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
        mLock         = lock;
    }

    @Override
    public void run() {
        Result result = Result.ErrorOpenPort;
        int code = StarResultCode.FAILURE;

        String message = "";

        synchronized (mLock) {
            try {
                if (mPort == null) {
                    if (mPortName == null) {
                        resultSendCallback(result, code, null, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }
                if (mPort == null) {
                    resultSendCallback(result, code, null, mCallback);
                    return;
                }

                result = Result.ErrorWritePort;

                StarPrinterStatus status = mPort.retreiveStatus();

                if (status.offline == true) {
                    throw new StarIOPortException("A printer is offline.");
                }

                byte[] commands = new byte[]{ 0x1b, 0x1d, 0x29, 0x4e, 0x02, 0x00, 0x49, 0x01};  //confirm SteadyLAN setting

                mPort.writePort(commands, 0, commands.length);

                List<Byte> receiveDataList = new ArrayList<>();
                byte[]     readBuffer      = new byte[1024];

                long start = System.currentTimeMillis();

                while (true) {
                    if (3000 < (System.currentTimeMillis() - start)) {
                        result = Result.ErrorReadPort;

                        throw new StarIOPortException("Timeout");
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }

                    int receiveSize = mPort.readPort(readBuffer, 0, readBuffer.length);

                    if (0 < receiveSize) {
                        for (int i = 0; i < receiveSize; i++) {
                            receiveDataList.add(readBuffer[i]);
                        }
                    }
                    else {
                        continue;
                    }

                    byte[] receiveData = new byte[receiveDataList.size()];

                    int receiveDataLength = receiveDataList.size();

                    for (int i = 0; i < receiveDataLength; i++) {
                        receiveData[i] = receiveDataList.get(i);
                    }

                    boolean receiveResponse = false;

                    //Check the steadyLAN setting value
                    //The following format is transmitted.
                    //  0x1b 0x1d 0x29 0x4e 0x02 0x00 0x49 0x01 [n] 0x0a 0x00
                    //The value of [n] indicates the SteadyLAN setting.
                    //  0x00: Invalid, 0x01: Valid(For iOS), 0x02: Valid(For Android), 0x03: Valid(For Windows)
                    if (receiveData.length >= 11){
                        for (int i = 0; i < receiveData.length; i++){
                            if (receiveData[i + 0] == 0x1b &&
                                    receiveData[i + 1] == 0x1d &&
                                    receiveData[i + 2] == 0x29 &&
                                    receiveData[i + 3] == 0x4e &&
                                    receiveData[i + 4] == 0x02 &&
                                    receiveData[i + 5] == 0x00 &&
                                    receiveData[i + 6] == 0x49 &&
                                    receiveData[i + 7] == 0x01 &&
                                    //  receiveData[i + 8] is stored the steadylan setting value.
                                    receiveData[i + 9] == 0x0a &&
                                    receiveData[i + 10] == 0x00) {

                                switch (receiveData[i + 8]) {
                                //  case 0x00:
                                    default:
                                        message = "SteadyLAN(Disable).";
                                        break;
                                    case 0x01:
                                        message = "SteadyLAN(for iOS).";
                                        break;
                                    case 0x02:
                                        message = "SteadyLAN(for Android).";
                                        break;
                                    case 0x03:
                                        message = "SteadyLAN(for Windows).";
                                        break;
                                }

                                receiveResponse = true;
                                break;
                            }

                        }
                    }

                    if (receiveResponse) {
                        result = Result.Success;
                        code = StarResultCode.SUCCESS;
                        break;
                    }
                }

            } catch (StarIOPortException e) {
                code = e.getErrorCode();
            }

            if (mPort != null && mPortName != null) {
                try {
                    StarIOPort.releasePort(mPort);
                } catch (StarIOPortException e) {
                    // Nothing
                }
                mPort = null;
            }

            resultSendCallback(result, code, message, mCallback);
        }
    }

    private static void resultSendCallback(final Result result, final int code, final String remoteConfigSetting, final Communication.SteadyLANSettingCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRemoteConfigSetting(new CommunicationResult(result, code), remoteConfigSetting);
                }
            });
        }
    }
}

public class Communication {

    public static class CommunicationResult {
        private Result mResult = Result.ErrorUnknown;
        private int    mCode   = StarResultCode.FAILURE;

        public CommunicationResult(Result result, int code) {
            mResult = result;
            mCode   = code;
        }

        public Result getResult() {
            return mResult;
        }

        public int getCode() {
            return mCode;
        }
    }

    public enum Result {
        Success,
        ErrorUnknown,
        ErrorOpenPort,
        ErrorBeginCheckedBlock,
        ErrorEndCheckedBlock,
        ErrorWritePort,
        ErrorReadPort,
    }

    interface SteadyLANSettingCallback {
        void onRemoteConfigSetting(CommunicationResult communicationResult, String remoteConfigSetting);
    }

    interface SendCallback {
        void onStatus(CommunicationResult communicationResult);
    }

    public static void sendCommands(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, SendCallback callback) {
        SendCommandThread thread = new SendCommandThread(lock, commands, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void confirmSteadyLANSetting(Object lock, String portName, String portSettings, int timeout, Context context, SteadyLANSettingCallback callback) {
        SteadyLANSettingThread thread = new SteadyLANSettingThread(lock, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static String getCommunicationResultMessage(CommunicationResult communicationResult) {
        StringBuilder builder = new StringBuilder();

        switch (communicationResult.getResult()) {
            case Success:
                builder.append("Success!");
                break;
            case ErrorOpenPort:
                builder.append("Fail to openPort");
                break;
            case ErrorBeginCheckedBlock:
                builder.append("Printer is offline (beginCheckedBlock)");
                break;
            case ErrorEndCheckedBlock:
                builder.append("Printer is offline (endCheckedBlock)");
                break;
            case ErrorReadPort:
                builder.append("Read port error (readPort)");
                break;
            case ErrorWritePort:
                builder.append("Write port error (writePort)");
                break;
            default:
                builder.append("Unknown error");
                break;
        }

        if (communicationResult.getResult() != Result.Success) {
            builder.append("\n\nError code: ");
            builder.append(communicationResult.getCode());

            if (communicationResult.getCode() == StarResultCode.FAILURE) {
                builder.append(" (Failed)");
            }
            else if (communicationResult.getCode() == StarResultCode.FAILURE_IN_USE) {
                builder.append(" (In use)");
            }
            else if (communicationResult.getCode() == StarResultCode.FAILURE_PAPER_PRESENT) {
                builder.append(" (Paper present)");
            }
        }

        return builder.toString();
    }

}