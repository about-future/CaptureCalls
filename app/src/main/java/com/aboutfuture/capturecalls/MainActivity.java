package com.aboutfuture.capturecalls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        View.OnTouchListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String PHONE_NUMBER_KEY = "number_key";
    private static final String CALL_CHRONOMETER_KEY = "chronometer_key";
    private static final String IS_BUTTON_RED_KEY = "is_red_key";
    private static final int REQUEST_PERMISSION_READ_PHONE_STATE = 0;
    private static final int REQUEST_PERMISSION_ANSWER_PHONE_CALLS = 1;
    private static final int REQUEST_PERMISSION_CALL_PHONE = 2;
    private static final int REQUEST_PERMISSION_MODIFY_AUDIO_SETTINGS = 3;

    private ConstraintLayout mMainLayout;
    private ImageView mCallerImageView;
    private TextView mPhoneNumberTextView;
    private TextView mIncomingCallTextView;
    private ImageView mIncomingCallImageView;
    private ImageView mRejectCallImageView;
    private ImageView mAnswerCallImageView;
    private boolean mIsRed = false;
    private TelephonyManager telephonyManager;
    private Handler mIconHandler;
    private Runnable mIconRunnable;
    private Chronometer mChronometer;
    private String mPhoneNumber = "******1508";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainLayout = findViewById(R.id.main_layout);
        mCallerImageView = findViewById(R.id.caller_iv);
        mPhoneNumberTextView = findViewById(R.id.phone_number_tv);
        mIncomingCallTextView = findViewById(R.id.call_incoming_tv);
        mChronometer = findViewById(R.id.call_chronometer);
        mIncomingCallImageView = findViewById(R.id.call_incoming_iv);
        mRejectCallImageView = findViewById(R.id.call_reject_iv);
        mAnswerCallImageView = findViewById(R.id.call_answer_iv);

        mIconHandler = new Handler();
        if (!mIsRed) {
            // Shake 5 times the RingingPhone image
            shakeIcon();
        } else {
            mIconHandler.removeCallbacks(mIconRunnable);
        }

        /////////////////////////////////////////////////////////////////////////////

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission has not been granted, therefore prompt the user to grant permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQUEST_PERMISSION_READ_PHONE_STATE);
            }
        }

        // TODO: This is the new version!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Read phone state permission has not been granted yet.
                requestReadPhoneStatePermission();
            }
        }

        // TODO: set permissions

        if (Build.VERSION.SDK_INT >= 23) { // Necessary permission
            if (checkSelfPermission("android.permission.SYSTEM_ALERT_WINDOW") != PackageManager.PERMISSION_GRANTED) {
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(myIntent);
            }
            if (Build.VERSION.SDK_INT < 26) { // Permissions for Android 6.x and 7.x
                ContentResolver contentResolver = getContentResolver();
                String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
                String packageName = getPackageName();
                if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
                    Intent intent2 = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent2);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 26) { // Necessary permission for SDKs 26 and up
            if (checkSelfPermission("android.permission.ANSWER_PHONE_CALLS") != PackageManager.PERMISSION_GRANTED) {
                // Permission has not been granted, therefore prompt the user to grant permission
                String szPermissions[] = {"android.permission.ANSWER_PHONE_CALLS"};
                requestPermissions(szPermissions, 0);
            }
        }

        /////////////////////////////////////////////////////////////


        if (getIntent() != null) {
            Intent intent = getIntent();
            if (intent.hasExtra(getString(R.string.phone_number_key))) {
                mPhoneNumber = intent.getStringExtra(getString(R.string.phone_number_key));
                mPhoneNumberTextView.setText(mPhoneNumber);
            }
        }

        mPhoneNumberTextView.setText(mPhoneNumber);

        // Listen and if the call ends, close this activity
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //finish();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Check for speaker permissions

                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        mIncomingCallImageView.setOnTouchListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(PHONE_NUMBER_KEY, mPhoneNumber);
        outState.putBoolean(IS_BUTTON_RED_KEY, mIsRed);
        if (mIsRed) {
            outState.putLong(CALL_CHRONOMETER_KEY, mChronometer.getBase());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(PHONE_NUMBER_KEY)) {
            mPhoneNumber = savedInstanceState.getString(PHONE_NUMBER_KEY);
            mPhoneNumberTextView.setText(mPhoneNumber);
        }

        if (savedInstanceState.containsKey(IS_BUTTON_RED_KEY)) {
            mIsRed = savedInstanceState.getBoolean(IS_BUTTON_RED_KEY);
            if (mIsRed) {
                mIncomingCallImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_call_end));
                mIncomingCallImageView.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_red));
                mIncomingCallImageView.setColorFilter(0xFFFFFFFF);
                mIconHandler.removeCallbacks(mIconRunnable);
                mIncomingCallTextView.setVisibility(View.INVISIBLE);

                // Continue chronometer clock
                mChronometer.setVisibility(View.VISIBLE);
                mChronometer.setBase(savedInstanceState.getLong(CALL_CHRONOMETER_KEY, 0));
                mChronometer.start();
            } else {
                mIncomingCallTextView.setVisibility(View.VISIBLE);
                mChronometer.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Just disable the normal functionality of backPressed button
        //super.onBackPressed();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        // Get x.y coordinates position of finger on screen
        float x = motionEvent.getRawX();
        float y = motionEvent.getRawY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If finger is touching the screen and the call was not answered, hide
                // incoming call icon and show Reject and Answer icons
                if (!mIsRed) {
                    mIncomingCallImageView.setVisibility(View.INVISIBLE);
                    mAnswerCallImageView.setVisibility(View.VISIBLE);
                    mRejectCallImageView.setVisibility(View.VISIBLE);

                    // Stop icon shaking
                    mIconHandler.removeCallbacks(mIconRunnable);
                }
                return true;

            case MotionEvent.ACTION_UP:
                // If finger stopped on Reject
                if ((mRejectCallImageView.getLeft() <= x && x <= mRejectCallImageView.getRight()) &&
                        (mRejectCallImageView.getTop() <= y && y <= mRejectCallImageView.getBottom())) {

                    // Hide views and set background color as red
                    mMainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed));
                    mCallerImageView.setVisibility(View.INVISIBLE);
                    mIncomingCallTextView.setVisibility(View.INVISIBLE);
                    mAnswerCallImageView.setVisibility(View.INVISIBLE);
                    mRejectCallImageView.setVisibility(View.INVISIBLE);

                    // End call, disable shake effect and delete number
                    endCall();
                    mPhoneNumber = "";
                    mPhoneNumberTextView.setText("");
                    mIconHandler.removeCallbacks(mIconRunnable);
                } else
                    // Otherwise, if finger stopped on Answer
                    if ((mAnswerCallImageView.getLeft() <= x && x <= mAnswerCallImageView.getRight()) &&
                            (mAnswerCallImageView.getTop() <= y && y <= mAnswerCallImageView.getBottom())) {
                        // Set background for incoming call button to red and image as ic_call_end
                        mIncomingCallImageView.setVisibility(View.VISIBLE);
                        mIncomingCallImageView.setColorFilter(0xFFFFFFFF);
                        mIncomingCallImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_call_end));
                        mIncomingCallImageView.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_red));

                        // Set mIsRed as true, so we'll know the call was answered
                        // and main button is now colored in red
                        mIsRed = true;

                        // Start call
                        startCall();

                        // Hide incoming call label and show chronometer and start it
                        mIncomingCallTextView.setVisibility(View.INVISIBLE);
                        mChronometer.setVisibility(View.VISIBLE);
                        mChronometer.setBase(SystemClock.elapsedRealtime());
                        mChronometer.start();

                        // Disable shake effect
                        mIconHandler.removeCallbacks(mIconRunnable);

                        // Reset views
                        mAnswerCallImageView.setColorFilter(0x9900FF00);
                        mAnswerCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                        mAnswerCallImageView.setVisibility(View.INVISIBLE);
                        mRejectCallImageView.setVisibility(View.INVISIBLE);
                    } else {
                        // Otherwise, if the call was started and finger stopped on the end call button
                        if (mIsRed && (mIncomingCallImageView.getLeft() <= x && x <= mIncomingCallImageView.getRight()) &&
                                (mIncomingCallImageView.getTop() <= y && y <= mIncomingCallImageView.getBottom())) {
                            // Hide button and set the entire background to red
                            mIncomingCallImageView.setVisibility(View.INVISIBLE);
                            mMainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed));
                            mCallerImageView.setVisibility(View.INVISIBLE);
                            mIncomingCallTextView.setVisibility(View.INVISIBLE);

                            // Reset chronometer and hide it
                            mChronometer.setVisibility(View.INVISIBLE);
                            mChronometer.stop();
                            mChronometer.setBase(SystemClock.elapsedRealtime());

                            // End call and reset chronometer
                            endCall();

                            // Disable shake effect and delete number
                            mIconHandler.removeCallbacks(mIconRunnable);
                            mPhoneNumber = "";
                            mPhoneNumberTextView.setText("");
                        } else {
                            // Otherwise, if the call was never started and motion was canceled
                            // somewhere on the screen, but not on any button, reset all views
                            mIncomingCallImageView.setVisibility(View.VISIBLE);
                            mAnswerCallImageView.setVisibility(View.INVISIBLE);
                            mRejectCallImageView.setVisibility(View.INVISIBLE);

                            mAnswerCallImageView.setColorFilter(0x9900FF00);
                            mAnswerCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
                            mRejectCallImageView.setColorFilter(0x99FF0000);
                            mRejectCallImageView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));

                            // Restart shake effect
                            if (!mIsRed) {
                                shakeIcon();
                            } else {
                                mIconHandler.removeCallbacks(mIconRunnable);
                            }
                        }
                    }
                return true;

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

                mIconHandler.removeCallbacks(mIconRunnable);

                return true;
        }

        view.performClick();
        return false;
    }

    private void startCall() {
        if (Build.VERSION.SDK_INT >= 26) { // Handles Android >= 8.0
            if (checkSelfPermission("android.permission.ANSWER_PHONE_CALLS") == PackageManager.PERMISSION_GRANTED) {
                TelecomManager tm = (TelecomManager) this.getSystemService(Context.TELECOM_SERVICE);
                if (tm != null)
                    tm.acceptRingingCall();
            }
        }
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 26) { // Hangup in Android 6.x and 7.x
            MediaSessionManager mediaSessionManager = (MediaSessionManager) getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                try {
                    List<MediaController> mediaControllerList = mediaSessionManager.getActiveSessions
                            (new ComponentName(getApplicationContext(), NotificationReceiverService.class));

                    for (android.media.session.MediaController m : mediaControllerList) {
                        if ("com.android.server.telecom".equals(m.getPackageName())) {
                            m.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
                            m.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
                            break;
                        }
                    }
                } catch (Exception e) { /* Do Nothing */ }
            }
        }

        if (Build.VERSION.SDK_INT < 23) { // Handles all SDKs up to Android 5.1
            try {
                if (Build.MANUFACTURER.equalsIgnoreCase("HTC")) { // Uniquement pour HTC
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    // TODO: Update this
                    if (audioManager != null && !audioManager.isWiredHeadsetOn()) {
                        Intent i = new Intent(Intent.ACTION_HEADSET_PLUG);
                        i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        i.putExtra("state", 0);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void endCall() {
        try {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                Class cls = Class.forName(telephonyManager.getClass().getName());

                Method method;
                method = cls.getDeclaredMethod("getITelephony");
                method.setAccessible(true);

                ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                telephonyService.endCall();

                // Reset chronometer and hide it
                mChronometer.setVisibility(View.INVISIBLE);
                mChronometer.stop();
                mChronometer.setBase(SystemClock.elapsedRealtime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shakeIcon() {
        Animation shake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);
        mIncomingCallImageView.startAnimation(shake);

        mIconRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    shakeIcon();
                } catch (NullPointerException e) {
                    Log.v("Shake Exception", e.toString());
                }
            }
        };

        mIconHandler.postDelayed(mIconRunnable, 3000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsRed) {
            shakeIcon();
        } else {
            mIconHandler.removeCallbacks(mIconRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIconHandler.removeCallbacks(mIconRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIconHandler.removeCallbacks(mIconRunnable);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Hide any window or activity that wants to take over
        if (!hasFocus) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }

    private void requestReadPhoneStatePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {
            Snackbar.make(mMainLayout, R.string.permission_read_phone_state,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_PHONE_STATE},
                                    REQUEST_PERMISSION_READ_PHONE_STATE);
                        }
                    })
                    .show();
        } else {
            // Read phone state permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PERMISSION_READ_PHONE_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_READ_PHONE_STATE) {
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Read phone state permission has been granted
                Snackbar.make(mMainLayout, R.string.permission_available_phone_state,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mMainLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();

            }

        } else if (requestCode == REQUEST_PERMISSION_ANSWER_PHONE_CALLS) {
            //
        } else if (requestCode == REQUEST_PERMISSION_CALL_PHONE) {
            //
        } else if (requestCode == REQUEST_PERMISSION_MODIFY_AUDIO_SETTINGS) {
            //
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
