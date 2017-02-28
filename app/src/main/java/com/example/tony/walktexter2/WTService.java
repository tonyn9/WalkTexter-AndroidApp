package com.example.tony.walktexter2;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class WTService extends Service {

    final int handlerState = 0;
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
    private StringBuilder recDataString = new StringBuilder();

    public WTService() {
    }

    //should receive data
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        WTService getService(){
            return WTService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Bluetooth Service","Service is created");
        Toast.makeText(this, "Service Created", Toast.LENGTH_LONG);
        stopThread = false;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        //launch new thread here for long tasks
        Log.d("Bluetooth Service", "Service Started");
        Toast.makeText(this, "Walk and Text Responsibly", Toast.LENGTH_LONG);
        bluetoothIn = new Handler(){

            public void handleMesage(android.os.Message msg){
                Log.d("Debug","handle Message");
                if (msg.what == handlerState){
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);

                    Log.d("Recorded", recDataString.toString());

                    //do stuff here
                    //Notify User Here
                    String arr[] = readMessage.split(":");
                    if (arr[0] == "status" && arr[1] == "warning" ){
                        //launch notification
                        WTNotifications alert = new WTNotifications();
                        alert.notify(getApplicationContext(), "Object Ahead", NotificationID);
                    }else if (arr[1] == "test"){
                        // ignore?
                        Toast.makeText(getApplicationContext(), "We Guchi", Toast.LENGTH_LONG);
                    }


                    recDataString.delete(0, recDataString.length()); // deletes string data
                }
            }

        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBlueToothState();
        return super.onStartCommand(intent, flags, startId);
    }

    //checks if bluetooth is here and available
    private void checkBlueToothState() {
        if (btAdapter == null){
            Log.d("Bluetooth Service" , "Bluetooth adapter missing. Device not supported");
            Toast.makeText(WTService.this, "Bluetooth adapter missing. Device not supported.", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "problem connecting to device", Toast.LENGTH_LONG);
                    stopSelf();
                }
            } else{
                Log.d("Bluetooth Service", "Bluetooth not on. Please turn on");
                Toast.makeText(WTService.this, "turn on bluetooth", Toast.LENGTH_LONG).show();
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy(){
        // before destroying self, send a close message
        if (CedT != null){
            // tell the pi to shut down before closing the stream
            CedT.write("Close");
        }
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
                Toast.makeText(WTService.this, "Connected to " + Device.getName(), Toast.LENGTH_LONG).show();
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

        public void run(){
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

                    String arr[] = readMessage.split(":");
                    if (arr[0].equals("status") && arr[1].equals("warning") ){
                        //launch notification
                        Log.d("Notification services", "Making Notifications");
                        /*
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(getApplicationContext())
                                        .setSmallIcon(R.drawable.ic_stat_wts)
                                        .setContentTitle("My notification")
                                        .setContentText("Watch out yo");*/
                        //int mNotificationId = 060;
                        //NotificationManager mNotifyMgr =
                        //        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        Log.d("Notification services", "Am i getting this far?");
                        //mNotifyMgr.notify(mNotificationId, mBuilder.build());
                        WTNotifications alert = new WTNotifications();
                        alert.notify(getApplicationContext(), "Object Ahead", NotificationID);
                    }else if (arr[1].equals( "test")){
                        // ignore?
                        Toast.makeText(getApplicationContext(), "We Guchi", Toast.LENGTH_LONG);
                    }else{
                        Log.d("message", "arr[0] : " + arr[0] + " arr[1] " +arr[1]);
                    }

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
