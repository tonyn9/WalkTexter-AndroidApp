package com.example.tony.walktexter2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    //Floating action buttons for plus and moreinfo
    FloatingActionButton fab_plus, fab_moreinfo;
    Animation FabOpen, FabClose, FabClockwise, FabCounterclockwise;
    TextView howto;
    boolean isOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /********** For Floating Action Buttons **********/
        fab_plus = (FloatingActionButton)findViewById(R.id.fab_plus);
        fab_moreinfo = (FloatingActionButton)findViewById(R.id.fab_moreinfo);
        howto = (TextView)findViewById(R.id.fab_text);

        FabOpen = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_open);
        FabClose = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
        FabClockwise = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_clockwise);
        FabCounterclockwise = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_counterclockwise);

        fab_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isOpen) {
                    howto.startAnimation(FabClose);
                    fab_moreinfo.startAnimation(FabClose);
                    fab_plus.startAnimation(FabCounterclockwise);
                    fab_moreinfo.setClickable(false);
                    isOpen = false;
                } else {
                    howto.startAnimation(FabOpen);
                    fab_moreinfo.startAnimation(FabOpen);
                    fab_plus.startAnimation(FabClockwise);
                    fab_moreinfo.setClickable(true);
                    isOpen = true;
                }
            }
        });

        fab_moreinfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent webpageIntent = getOpenWebpageIntent(MainActivity.this);
                startActivity(webpageIntent);
            }
        });
        /********** End of For Floating Action Buttons **********/

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

    /********** For Floating Action Buttons -- MoreInfo **********/
    public static Intent getOpenWebpageIntent(Context context) {

        try {
            //context.getPackageManager()
              //      .getPackageInfo("com.twitter.android", 0); //Checks if app is installed.
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://srproj.eecs.uci.edu/projects/project-group-60-walktexter")); //Trys to make intent with webpage's URI
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://srproj.eecs.uci.edu/projects/project-group-60-walktexter")); //catches and opens a url to the desired page
        }
    }
    /********** End of For Floating Action Buttons -- MoreInfo **********/
}
