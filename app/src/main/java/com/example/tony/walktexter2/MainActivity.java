package com.example.tony.walktexter2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //start the service
    public void startWTService(View view){
        startService(new Intent(this, WTService.class));
    }

    //stop the service
    public void stopWTService(View view){
        stopService(new Intent(this, WTService.class));
        finish();
    }
}
