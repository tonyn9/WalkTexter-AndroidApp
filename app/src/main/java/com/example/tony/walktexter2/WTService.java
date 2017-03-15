package com.example.tony.walktexter2;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class WTService extends Service {


    Handler bluetoothIn;
    BluetoothDevice Device;
    private BluetoothAdapter btAdapter = null;
    Set<BluetoothDevice> pairedDevices;
    // Set notification ID so we can update it
    int NotificationID = 60;

    private static final UUID BTUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectingThread CingT;
    private ConnectedThread CedT;
    private boolean stopThread;


    public WTService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Bluetooth Service","Service is created");
        //Toast.makeText(this, "Service Created", Toast.LENGTH_LONG).show();
        stopThread = false;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        //launch new thread here for long tasks
        Log.d("Bluetooth Service", "Service Started");
        Toast.makeText(this, "Walk and Text Responsibly", Toast.LENGTH_LONG);
        bluetoothIn = new Handler(){
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBlueToothState();
        return super.onStartCommand(intent, flags, startId);
    }

    //checks if bluetooth is here and available
    private void checkBlueToothState() {
        if (btAdapter == null){
            Log.d("Bluetooth Service" , "Bluetooth adapter missing. Device not supported");
            Toast.makeText(this, "Device not supported. Bluetooth adapter missing.", Toast.LENGTH_LONG).show();
            stopSelf();
        } else{
            if (btAdapter.isEnabled()) {
                Log.d("Debug Bluetooth", "Bluetooth enabled. BT adress : " + btAdapter.getAddress()
                        + " BT name : " + btAdapter.getName());
                try{
                    // try to connect
                    pairedDevices = btAdapter.getBondedDevices();
                    for(BluetoothDevice bt : pairedDevices) {
                        // add names
                        if(bt.getName().equals("rpi-TeamSalt")
                                || bt.getName().equals(("raspberrypi"))){

                            Device = bt;
                            Log.d("Bluetooth Service", "Device bt is " + Device.getName());
                        }
                    }
                    CingT = new ConnectingThread(Device);
                    CingT.start();
                } catch(IllegalArgumentException e){
                    Log.d("Debug Bluetooth", "Problem Connecting");
                    Toast.makeText(this, "Problem connecting to device", Toast.LENGTH_LONG).show();
                    stopSelf();
                }
            } else{
                Log.d("Bluetooth Service", "Bluetooth not on. Please turn on");
                Toast.makeText(this, "Bluetooth not enabled. Please turn on.", Toast.LENGTH_LONG).show();
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        if (CedT != null){
            CedT.closeStreams();
        }
        if (CingT != null){
            CingT.closeSocket();
        }
        Log.d("Service","Destroying");
    }

    //inner private class for connecting thread
    private class ConnectingThread extends Thread{
        private final BluetoothSocket Socket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            Log.d("Debug Bluetooth", "Inside connecting thread");
            BluetoothSocket temp = null;
            this.mmDevice = device;
            Log.d("Debug Bluetooth", "Bluetooth UUID : " + BTUUID);
            try{
                temp = device.createRfcommSocketToServiceRecord(BTUUID);
            }catch (IOException e) {
                Log.d("Debug Bluetooth", "SOCKET CREATION FAILED :" + e.toString());
                Log.d("Bluetooth Service", "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopSelf();
            }
            Socket = temp;
        }

        @Override
        public void run(){
            //super.run();
            Log.d("Debug Bluetooth","In Connecting thread run()");

            //cancels discovery usually disabled
            //btAdapter.cancelDiscovery();
            try {
                if (!Socket.isConnected()){
                    Log.d("Bluetooth Service", "Socket is not connected. Connecting...");
                    Socket.connect();
                }
                Log.d("Debug Bluetooth", " Bluetooth Socket Connected");
                CedT = new ConnectedThread(Socket);
                CedT.start();
                Log.d("Debug Bluetooth", "Connected thread started");
                CedT.write("test");
            }catch (IOException e){
                try{
                    Log.d("Debug Bluetooth" , "Socket Connection Failed : " + e.toString());
                    Log.d("Bluetooth Service" , "Socket Connection failed, stopping service");
                    Socket.close();
                    stopSelf();
                }catch (IOException e2){
                    Log.d("Debug Bluetooth" , "Socket closing failed : " + e2.toString());
                    Log.d("Bluetooth Service" , "Socket closing failed, stopping service");
                    stopSelf();
                    //code ways to safely shut down
                    //finish();
                }
            }catch(IllegalStateException e){
                Log.d("Debug Bluetooth", "Connected thread failed to start : " + e.toString());
                Log.d("Bluetooth Service" , "Conned thread failed to start, stopping");
                stopSelf();
            }
        }

        public void closeSocket(){
            try{
                Socket.close();
            }catch (IOException e){
                Log.d("Debug Bluetooth", e.toString());
                Log.d("Bluetooth Service" , "Socket failed to close, stopping");
                stopSelf();
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final InputStream InStream;
        private final OutputStream OutStream;


        private String[] WarningArray;

        public ConnectedThread (BluetoothSocket socket){
            Log.d("Debug Bluetooth", "In Connected Thread");

            InputStream tempIn = null;
            OutputStream tempOut = null;

            try{
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e){
                Log.d("Debug Bluetooth", e.toString());
                Log.d("Bluetooth Service", "Unable to read/write, stopping");
                stopSelf();
            }

            InStream = tempIn;
            OutStream = tempOut;
        }

        private void WarningSetUp(){
            WarningArray = new String[4];
            WarningArray[0] = "Walk.";
            WarningArray[1] = "Don't Walk.";
            WarningArray[2] = "Stop Sign Ahead.";
            WarningArray[3] = "Object in front.";
        }

        public void run(){
            // setting up string
            WarningSetUp();

            Log.d("Debug Bluetooth","In connected thread run");
            byte[] buffer = new byte[256];
            int bytes;
            //keep looping to listen for received messages
            while (true && !stopThread){
                try {
                    Log.d("Bluetooth Service", "waiting for something");
                    bytes = InStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    Log.d("Debug Bluetooth listen", "Connected Thread : " + readMessage);

                    StringCheck(readMessage);

                    //send bytes to UI activity via handler
                    //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e){
                    Log.d("Debug Bluetooth", e.toString());
                    Log.d("Bluetooth Service", "unable to read during listen");
                    stopSelf();
                    break;
                }
            }
        }

        public void StringCheck(String check){
            Log.d("Debug String Check", "Connected Thread : " + check);
            String arr[] = check.split(":");
            // if format is status:warning:(int)
            // bit3 bit2 bit1 bit0
            // walk dont_walk stop sensor
            if (arr[0].equals("status") && arr[1].equals("warning")){
                try{
                    String bits = Integer.toBinaryString(Integer.parseInt(arr[2]));
                    // sets amount of bits
                    if(bits.length() < 4){
                        switch(bits.length()){
                            case (1) : bits = "000" + bits;
                                        break;
                            case (2) : bits = "00" + bits;
                                        break;
                            case (3) : bits = "0" + bits;
                                        break;
                            default:
                                break;
                        }
                    }


                    char bitMessage[] = bits.toCharArray();
                    Log.d("Bit array", String.valueOf(bitMessage));
                    String TitleWarning = "Warning: ";
                    for (int i = 0; i < bitMessage.length; i++){
                        //System.out.println("in for loop");
                        if (String.valueOf(bitMessage[i]).equals("1")){
                            Log.d("bitMessage for loop", WarningArray[i]);
                            TitleWarning += WarningArray[i];
                            TitleWarning += " ";
                            Log.d("Concat", "Concatted as: " + TitleWarning);
                        }
                    }
                    WTNotifications alert = new WTNotifications();
                    Log.d("Debug Title Warning", "Title is: " + TitleWarning);
                    alert.notify(getApplicationContext(), TitleWarning, NotificationID);
                }catch (IndexOutOfBoundsException e){
                    Log.d("Debug Message", "No 3rd argument" + e.toString());
                }catch (NumberFormatException num){
                    Log.d("Debug Message", "Test app is funky" + num.toString());
                }
            }else if(arr[1].equals("test")){
                Log.d("message", "preliminary connection");
            }else{
                Log.d("message", "arr[0] : " + arr[0] + " arr[1] " +arr[1]);
            }
        }

        public void write(String input) {
            byte [] msgBuffer = input.getBytes();
            try{
                OutStream.write(msgBuffer);
            }catch (IOException e){
                //cannot write then close
                Log.d("Debug Bluetooth", " Unable to write" + e.toString());
                Log.d("Bluetooth Service" , "Unable to write, stopping");
                stopSelf();
            }
        }

        public void closeStreams(){
            try{
                InStream.close();
                OutStream.close();
            } catch(IOException e){
                Log.d("Debug Bluetooth", e.toString());
                Log.d("Bluetooth Service", "Stream close failed, stopping");
                stopSelf();
            }
        }
    }
}
