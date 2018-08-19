package com.aboutfuture.capturecalls;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private ConstraintLayout mMainLayout;
    private ImageView mRingingPhoneImageView;
    private ImageView mRejectCallImageView;
    private ImageView mAnswerCallImageView;
    private boolean mIsRed = false;
    private TelephonyManager telephonyManager;

    // This is the gesture detector compat instance
    private GestureDetectorCompat gestureDetectorCompat = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainLayout = findViewById(R.id.main_layout);
        TextView phoneNumberTextView = findViewById(R.id.phone_number_tv);
        mRingingPhoneImageView = findViewById(R.id.phone_ringing_iv);
        mRejectCallImageView = findViewById(R.id.call_reject_iv);
        mAnswerCallImageView = findViewById(R.id.call_answer_iv);

        // Shake 5 times the RingingPhone image
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        mRingingPhoneImageView.startAnimation(shake);

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
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
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

        //mAnswerCallImageView.setOnTouchListener(this);
        mRingingPhoneImageView.setOnTouchListener(this);

//        mRejectCallImageView.setOnHoverListener(new View.OnHoverListener() {
//            @Override
//            public boolean onHover(View view, MotionEvent motionEvent) {
//                mRejectCallImageView.setColorFilter(0xFFFFFFFF);
//
////                try {
////                    if (telephonyManager != null) {
////                        Class clazz = Class.forName(telephonyManager.getClass().getName());
////
////                        Method method;
////                        method = clazz.getDeclaredMethod("getITelephony");
////                        method.setAccessible(true);
////
////                        ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
////                        telephonyService.endCall();
////                    }
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
//
//                return false;
//            }
//        });

//        mAnswerCallImageView.setOnHoverListener(new View.OnHoverListener() {
//            @Override
//            public boolean onHover(View view, MotionEvent motionEvent) {
//                mAnswerCallImageView.setColorFilter(0xFFFFFFFF);
//
////                if (Build.VERSION.SDK_INT < 23) { // Prend en charge jusqu'à Android 5.1
////                    try {
////                        if (Build.MANUFACTURER.equalsIgnoreCase("HTC")) { // Uniquement pour HTC
////                            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
////                            if (audioManager != null && !audioManager.isWiredHeadsetOn()) {
////                                Intent i = new Intent(Intent.ACTION_HEADSET_PLUG);
////                                i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
////                                i.putExtra("state", 0);
////                                i.putExtra("name", "Orasi");
////                                try {
////                                    sendOrderedBroadcast(i, null);
////                                } catch (Exception e) { /* Do Nothing */ }
////                            }
////                        }
////                        Runtime.getRuntime().exec("input keyevent " +
////                                Integer.toString(KeyEvent.KEYCODE_HEADSETHOOK));
////                    } catch (Exception e) {
////                        // Runtime.exec(String) had an I/O problem, try to fall back
////                        String enforcedPerm = "android.permission.CALL_PRIVILEGED";
////                        Intent btnDown = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
////                                Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN,
////                                        KeyEvent.KEYCODE_HEADSETHOOK));
////                        Intent btnUp = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
////                                Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
////                                        KeyEvent.KEYCODE_HEADSETHOOK));
////
////                        Context context = getApplicationContext();
////                        context.sendOrderedBroadcast(btnDown, enforcedPerm);
////                        context.sendOrderedBroadcast(btnUp, enforcedPerm);
////                    }
////                }
//
//                return false;
//            }
//        });

//        // Create a common gesture listener object
//        DetectSwipeGestureListener gestureListener = new DetectSwipeGestureListener();
//        // Set activity in the listener
//        //gestureListener.setImageViews(mRingingPhoneImageView, mAnswerCallImageView, mRejectCallImageView);
//        gestureListener.setImageView(this, mRingingPhoneImageView);
//        // Create the gesture detector with the gesture listener
//        gestureDetectorCompat = new GestureDetectorCompat(this, gestureListener);
//
//
//        mRingingPhoneImageView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                // Pass view on touch event to the gesture detector
//                gestureDetectorCompat.onTouchEvent(motionEvent);
//                // Return true to tell android OS that event has been consumed, do not pass it to other event listeners
//                return true;
//            }
//        });

//        GenericMotionListener(new View.OnGenericMotionListener() {
//            @Override
//            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
//                // Pass activity on touch event to the gesture detector
//                gestureDetectorCompat.onTouchEvent(motionEvent);
//                // Return true to tell android OS that event has been consumed, do not pass it to other event listeners
//                return true;
//            }
//        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        // Just disable backPressed button
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.v("EVENT", motionEvent.toString());
        float x = motionEvent.getRawX();
        float y = motionEvent.getRawY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mIsRed) {
                    mRingingPhoneImageView.setVisibility(View.INVISIBLE);
                    mAnswerCallImageView.setVisibility(View.VISIBLE);
                    mRejectCallImageView.setVisibility(View.VISIBLE);
                }
                break;
            case MotionEvent.ACTION_UP:
                if ((mRejectCallImageView.getLeft() <= x && x <= mRejectCallImageView.getRight()) &&
                        (mRejectCallImageView.getTop() <= y && y <= mRejectCallImageView.getBottom())) {
                    // Motion stopped on Reject. Hide views and set background color as red
                    mMainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed));
                    mAnswerCallImageView.setVisibility(View.INVISIBLE);
                    mRejectCallImageView.setVisibility(View.INVISIBLE);
                    // End call
                    endCall();
                } else if ((mAnswerCallImageView.getLeft() <= x && x <= mAnswerCallImageView.getRight()) &&
                        (mAnswerCallImageView.getTop() <= y && y <= mAnswerCallImageView.getBottom())) {
                    // Motion stopped on Answer. Set background ringing button to red and
                    // center image as call_end
                    mRingingPhoneImageView.setVisibility(View.VISIBLE);
                    mRingingPhoneImageView.setColorFilter(0xFFFFFFFF);
                    mRingingPhoneImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_call_end));
                    mRingingPhoneImageView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_red));
                    // Set mIsRed
                    mIsRed = true;
                    // Start call
                    startCall();
                    // Reset views
                    mAnswerCallImageView.setColorFilter(0x9900FF00);
                    mAnswerCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                    mAnswerCallImageView.setVisibility(View.INVISIBLE);
                    mRejectCallImageView.setVisibility(View.INVISIBLE);
                } else {
                    // If the call was started and finger stopped on the end call button,
                    // hide button and set background red
                    if (mIsRed && (mRingingPhoneImageView.getLeft() <= x && x <= mRingingPhoneImageView.getRight()) &&
                            (mRingingPhoneImageView.getTop() <= y && y <= mRingingPhoneImageView.getBottom())) {
                        mRingingPhoneImageView.setVisibility(View.INVISIBLE);
                        mMainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed));

                        endCall();

                    } else {
                        // Otherwise, the call was never started and motion was canceled somewhere
                        // on the background, so reset all views
                        mRingingPhoneImageView.setVisibility(View.VISIBLE);
                        mAnswerCallImageView.setVisibility(View.INVISIBLE);
                        mRejectCallImageView.setVisibility(View.INVISIBLE);

                        mAnswerCallImageView.setColorFilter(0x9900FF00);
                        mAnswerCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                        mRejectCallImageView.setColorFilter(0x99FF0000);
                        mRejectCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if ((mRejectCallImageView.getLeft() <= x && x <= mRejectCallImageView.getRight()) &&
                        (mRejectCallImageView.getTop() <= y && y <= mRejectCallImageView.getBottom())) {
                    Log.i("RINGING", "is currently above Reject");
                    mRejectCallImageView.setColorFilter(0xFFFFFFFF);
                    mRejectCallImageView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_red));
                    //mRejectCallImageView.dispatchTouchEvent(motionEvent);
                } else if ((mAnswerCallImageView.getLeft() <= x && x <= mAnswerCallImageView.getRight()) &&
                        (mAnswerCallImageView.getTop() <= y && y <= mAnswerCallImageView.getBottom())) {
                    Log.i("RINGING", "is currently above Answer");
                    mAnswerCallImageView.setColorFilter(0xFFFFFFFF);
                    mAnswerCallImageView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_green));
                    //mRejectCallImageView.dispatchTouchEvent(motionEvent);
                } else {
                    mAnswerCallImageView.setColorFilter(0x9900FF00);
                    mAnswerCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                    mRejectCallImageView.setColorFilter(0x99FF0000);
                    mRejectCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                }

                break;
        }

        view.performClick();

        return true;
    }

    private void startCall() {
        if (Build.VERSION.SDK_INT < 23) { // Prend en charge jusqu'à Android 5.1
            try {
                if (Build.MANUFACTURER.equalsIgnoreCase("HTC")) { // Uniquement pour HTC
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null && !audioManager.isWiredHeadsetOn()) {
                        Intent i = new Intent(Intent.ACTION_HEADSET_PLUG);
                        i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        i.putExtra("state", 0);
                        i.putExtra("name", "Orasi");
                        try {
                            sendOrderedBroadcast(i, null);
                        } catch (Exception e) { /* Do Nothing */ }
                    }
                }
                Runtime.getRuntime().exec("input keyevent " +
                        Integer.toString(KeyEvent.KEYCODE_HEADSETHOOK));
            } catch (Exception e) {
                // Runtime.exec(String) had an I/O problem, try to fall back
                String enforcedPerm = "android.permission.CALL_PRIVILEGED";
                Intent btnDown = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
                        Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_HEADSETHOOK));
                Intent btnUp = new Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
                        Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
                                KeyEvent.KEYCODE_HEADSETHOOK));

                Context context = getApplicationContext();
                context.sendOrderedBroadcast(btnDown, enforcedPerm);
                context.sendOrderedBroadcast(btnUp, enforcedPerm);
            }
        }
    }

    private void endCall() {
        try {
            if (telephonyManager != null) {
                Class clazz = Class.forName(telephonyManager.getClass().getName());

                Method method;
                method = clazz.getDeclaredMethod("getITelephony");
                method.setAccessible(true);

                ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                telephonyService.endCall();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
