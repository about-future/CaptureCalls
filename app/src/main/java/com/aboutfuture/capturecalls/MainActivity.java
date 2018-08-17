package com.aboutfuture.capturecalls;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView phoneNumberTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberTextView = findViewById(R.id.phone_number_tv);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
//                    != PackageManager.PERMISSION_GRANTED) {
//                // Permission has not been granted, therefore prompt the user to grant permission
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_PHONE_STATE},
//                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
//            }
//        }

        if (getIntent() != null) {
            Intent intent = getIntent();
            if (intent.hasExtra(getString(R.string.phone_number_key))) {
                String number = intent.getStringExtra(getString(R.string.phone_number_key));
                phoneNumberTextView.setText(number);
            } else {
                phoneNumberTextView.setText("No call yet!");
            }
        }

        // Listen and if the call ends, close this activity
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                //super.onCallStateChanged(state, incomingNumber);
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    finish();
                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        // Just disable backPressed button
    }
}
