package com.aboutfuture.capturecalls;

import android.app.ActionBar;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    //private TextView mPhoneNumberTextView;
    private ImageView mRingingPhoneImageView;
    private ImageView mRejectCallImageView;
    private ImageView mAnswerCallImageView;
    private boolean mVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView phoneNumberTextView = findViewById(R.id.phone_number_tv);
        mRingingPhoneImageView = findViewById(R.id.phone_ringing_iv);
        mRejectCallImageView = findViewById(R.id.call_reject_iv);
        mAnswerCallImageView = findViewById(R.id.call_answer_iv);

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
            }
        }

        // Listen and if the call ends, close this activity
        final TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                //super.onCallStateChanged(state, incomingNumber);
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //finish();
                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }


        mRingingPhoneImageView.setOnTouchListener(this);
        mAnswerCallImageView.setOnTouchListener(this);


        mRejectCallImageView.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View view, MotionEvent motionEvent) {
                mRejectCallImageView.setColorFilter(0xFFFFFFFF);

//                try {
//                    if (telephonyManager != null) {
//                        Class clazz = Class.forName(telephonyManager.getClass().getName());
//
//                        Method method;
//                        method = clazz.getDeclaredMethod("getITelephony");
//                        method.setAccessible(true);
//
//                        ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
//                        telephonyService.endCall();
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                return false;
            }
        });

        mAnswerCallImageView.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                mAnswerCallImageView.setColorFilter(0xFFFFFFFF);
                return false;
            }
        });

        mAnswerCallImageView.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View view, MotionEvent motionEvent) {
                mAnswerCallImageView.setColorFilter(0xFFFFFFFF);

//                if (Build.VERSION.SDK_INT < 23) { // Prend en charge jusqu'à Android 5.1
//                    try {
//                        if (Build.MANUFACTURER.equalsIgnoreCase("HTC")) { // Uniquement pour HTC
//                            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                            if (audioManager != null && !audioManager.isWiredHeadsetOn()) {
//                                Intent i = new Intent(Intent.ACTION_HEADSET_PLUG);
//                                i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
//                                i.putExtra("state", 0);
//                                i.putExtra("name", "Orasi");
//                                try {
//                                    sendOrderedBroadcast(i, null);
//                                } catch (Exception e) { /* Do Nothing */ }
//                            }
//                        }
//                        Runtime.getRuntime().exec("input keyevent " +
//                                Integer.toString(KeyEvent.KEYCODE_HEADSETHOOK));
//                    } catch (Exception e) {
//                        // Runtime.exec(String) had an I/O problem, try to fall back
//                        String enforcedPerm = "android.permission.CALL_PRIVILEGED";
//                        Intent btnDown = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
//                                Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN,
//                                        KeyEvent.KEYCODE_HEADSETHOOK));
//                        Intent btnUp = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
//                                Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
//                                        KeyEvent.KEYCODE_HEADSETHOOK));
//
//                        Context context = getApplicationContext();
//                        context.sendOrderedBroadcast(btnDown, enforcedPerm);
//                        context.sendOrderedBroadcast(btnUp, enforcedPerm);
//                    }
//                }

                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        // Just disable backPressed button
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.v("EVENT", motionEvent.toString());

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mRingingPhoneImageView.setVisibility(View.INVISIBLE);
                mAnswerCallImageView.setVisibility(View.VISIBLE);
                mRejectCallImageView.setVisibility(View.VISIBLE);
                break;
            case MotionEvent.ACTION_UP:
                mRingingPhoneImageView.setVisibility(View.VISIBLE);
                mAnswerCallImageView.setVisibility(View.INVISIBLE);
                mRejectCallImageView.setVisibility(View.INVISIBLE);
                break;
        }

        return true;
    }
}
