package com.example.tony.walktexter2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    Intent intent;
    TextView StatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatusText = (TextView) findViewById(R.id.StatusText);
        intent = null;
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
        StatusText.setVisibility(View.VISIBLE);
        if(intent == null){
            StatusText.setText("Status : Disconnected");
        }else{
            StatusText.setText("Status : Unknown");
        }
    }
}
