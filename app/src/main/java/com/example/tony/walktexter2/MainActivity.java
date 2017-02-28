package com.example.tony.walktexter2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //start the service
    public void startWTService(View view){
        intent = new Intent(this, WTService.class);
        startService(intent);
    }

    //stop the service
    public void stopWTService(View view){
        if(intent != null){
            stopService(intent);
        }
        finish();
    }

    //send a message to the service to get data.
    //for ex:
    //if disconnected
    //if connected, get name of device connected
    public void statusWTService(View view){

    }
}
