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
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        View.OnTouchListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String PHONE_NUMBER_KEY = "number_key";
    private static final String CALL_CHRONOMETER_KEY = "chronometer_key";
    private static final String IS_BUTTON_RED_KEY = "is_red_key";
    private static final String IS_SPEAKER_ON_KEY = "is_speaker_key";
    private static final String IS_MIC_ON_KEY = "is_mic_key";

    private static final int REQUEST_CODE_FOR_READ_PHONE_STATE = 0;
    private static final int REQUEST_CODE_FOR_ANSWER_PHONE_CALLS = 1;
    private static final int REQUEST_CODE_FOR_CALL_PHONE = 2;
    private static final int REQUEST_CODE_FOR_MODIFY_AUDIO_SETTINGS = 3;
    private static final int REQUEST_CODE_FOR_GROUP = 4;

    @BindView(R.id.main_layout)
    ConstraintLayout mMainLayout;
    @BindView(R.id.caller_iv)
    ImageView mCallerImageView;
    @BindView(R.id.phone_number_tv)
    TextView mPhoneNumberTextView;
    @BindView(R.id.call_incoming_tv)
    TextView mIncomingCallTextView;
    @BindView(R.id.call_incoming_iv)
    ImageView mIncomingCallImageView;
    @BindView(R.id.call_reject_iv)
    ImageView mRejectCallImageView;
    @BindView(R.id.call_answer_iv)
    ImageView mAnswerCallImageView;
    @BindView(R.id.circle_line)
    View mCircleLine;
    @BindView(R.id.call_chronometer)
    Chronometer mChronometer;
    @BindView(R.id.speaker_iv)
    ImageView mSpeakerImageView;
    @BindView(R.id.microphone_iv)
    ImageView mMicrophoneImageView;

    private boolean mIsRed = false;
    private TelephonyManager telephonyManager;
    private Handler mIconHandler;
    private Runnable mIconRunnable;
    private String mPhoneNumber = "******1508";
    private boolean mIsSpeakerOn = false;
    private boolean mIsMicOn = true;
    private AudioManager mAudioManager;

    private static String[] PERMISSIONS_LIST = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS};

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // To be able to draw a perfect circle when the incomingCall image view is touched,
        // we get the screen position of rejectCall and answerCall image views, extract the
        // x value from both and set the difference between them as the height of our circle.
        // This way, no mather how big or small is our screen, the circle will always be perfect.
        mMainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                mMainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int[] rejectLocations = new int[2];
                int[] answerLocations = new int[2];
                mRejectCallImageView.getLocationOnScreen(rejectLocations);
                mAnswerCallImageView.getLocationOnScreen(answerLocations);

                ConstraintLayout.LayoutParams paramsCircle =
                        (ConstraintLayout.LayoutParams) mCircleLine.getLayoutParams();
                paramsCircle.height = answerLocations[0] - rejectLocations[0];
                mCircleLine.setLayoutParams(paramsCircle);
            }
        });

        // Start shaking 5 times the mIncomingCallImageView every 3 seconds
        mIconHandler = new Handler();
        if (!mIsRed) {
            shakeIcon();
        } else {
            mIconHandler.removeCallbacks(mIconRunnable);
        }

        // Handle needed permissions
        if (Build.VERSION.SDK_INT >= 23) {
            // Permission to read the status of any ongoing calls
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                // READ_PHONE_STATE permission has not been granted yet.
                requestReadPhoneStatePermission();
            }

            // Permission to make and end a phone call
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                // CALL_PHONE permission has not been granted yet.
                requestCallPhonePermission();
            }

            //Permission to modify audio settings (turn on phone speaker, mute microphone,...)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    != PackageManager.PERMISSION_GRANTED) {
                // MODIFY_AUDIO_SETTINGS permission has not been granted yet.
                requestModifyAudioSettingsPermission();
            }

//            // Permission to answer incoming phone calls
//            if (checkSelfPermission("android.permission.SYSTEM_ALERT_WINDOW") != PackageManager.PERMISSION_GRANTED) {
//                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                startActivity(myIntent);
//            }
            if (Build.VERSION.SDK_INT < 26) { // Permissions for Android 6.x and 7.x
                ContentResolver contentResolver = getContentResolver();
                String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
                String packageName = getPackageName();
                if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
                    Intent intent2 = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent2);
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS)
                        != PackageManager.PERMISSION_GRANTED) {
                    // ANSWER_PHONE_CALLS permission has not been granted yet.
                    requestAnswerPhoneCallsPermission();
                }
            }
        }

        // Get the hidden number that was passed by the CallReceiver
        if (getIntent() != null) {
            Intent intent = getIntent();
            if (intent.hasExtra(getString(R.string.phone_number_key))) {
                mPhoneNumber = intent.getStringExtra(getString(R.string.phone_number_key));
                mPhoneNumberTextView.setText(mPhoneNumber);
            }
        }

        // Turn speaker on or off
        mSpeakerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsSpeakerOn) {
                    mSpeakerImageView.setAlpha(0.5f);
                } else {
                    mSpeakerImageView.setAlpha(1f);
                }
                mIsSpeakerOn = !mIsSpeakerOn;
                mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                mAudioManager.setSpeakerphoneOn(mIsSpeakerOn);
            }
        });

        // Turn mic on or off
        mMicrophoneImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsMicOn) {
                    mMicrophoneImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic));
                } else {
                    mMicrophoneImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_off));
                }
                mIsMicOn = !mIsMicOn;
                mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                mAudioManager.setMicrophoneMute(mIsMicOn);
            }
        });

        // Listen and if the call ends, close this activity after 0.5 seconds
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    // Hide the main layout
                    mMainLayout.setVisibility(View.INVISIBLE);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //finish();
                        }
                    }, 500);
                } else if (state == TelephonyManager.CALL_STATE_RINGING) {
                    // If phone is ringing, speaker and mic can't be clicked
                    mSpeakerImageView.setEnabled(false);
                    mMicrophoneImageView.setEnabled(false);
                    // Their color should be faded
                    mSpeakerImageView.setAlpha(0.5f);
                    mMicrophoneImageView.setAlpha(0.5f);
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // If the call was answered, enable both buttons
                    mSpeakerImageView.setEnabled(true);
                    mMicrophoneImageView.setEnabled(true);
                    // Microphone is on by default and shouldn't be faded anymore
                    mMicrophoneImageView.setAlpha(1f);
                }
            }
        };

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // Set a touch listener on incoming call image view
        mIncomingCallImageView.setOnTouchListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(PHONE_NUMBER_KEY, mPhoneNumber);
        outState.putBoolean(IS_BUTTON_RED_KEY, mIsRed);
        if (mIsRed) {
            outState.putLong(CALL_CHRONOMETER_KEY, mChronometer.getBase());
        }
        outState.putBoolean(IS_SPEAKER_ON_KEY, mIsSpeakerOn);
        outState.putBoolean(IS_MIC_ON_KEY, mIsMicOn);

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

        if (savedInstanceState.containsKey(IS_SPEAKER_ON_KEY)) {
            mIsSpeakerOn = savedInstanceState.getBoolean(IS_SPEAKER_ON_KEY);
            if (mIsSpeakerOn) {
                mSpeakerImageView.setAlpha(1f);
            } else {
                mSpeakerImageView.setAlpha(0.5f);
            }
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            mAudioManager.setSpeakerphoneOn(mIsSpeakerOn);
        }

        if (savedInstanceState.containsKey(IS_MIC_ON_KEY)) {
            mIsMicOn = savedInstanceState.getBoolean(IS_MIC_ON_KEY);
            if (mIsMicOn) {
                mMicrophoneImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_off));
            } else {
                mMicrophoneImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic));
            }
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            mAudioManager.setMicrophoneMute(mIsMicOn);
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
                    mCircleLine.setVisibility(View.VISIBLE);

                    // Stop icon shaking
                    mIconHandler.removeCallbacks(mIconRunnable);
                }
                return true;

            case MotionEvent.ACTION_UP:
                // If finger stopped on Reject
                if ((mRejectCallImageView.getLeft() <= x && x <= mRejectCallImageView.getRight()) &&
                        (mRejectCallImageView.getTop() <= y && y <= mRejectCallImageView.getBottom())) {

                    // Hide views and set background color as red
                    //mMainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed));
                    mMainLayout.setVisibility(View.INVISIBLE);
                    mCallerImageView.setVisibility(View.INVISIBLE);
                    mIncomingCallTextView.setVisibility(View.INVISIBLE);
                    mAnswerCallImageView.setVisibility(View.INVISIBLE);
                    mRejectCallImageView.setVisibility(View.INVISIBLE);
                    mCircleLine.setVisibility(View.INVISIBLE);

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
                        mCircleLine.setVisibility(View.INVISIBLE);
                    } else {
                        // Otherwise, if the call was started and finger stopped on the end call button
                        if (mIsRed && (mIncomingCallImageView.getLeft() <= x && x <= mIncomingCallImageView.getRight()) &&
                                (mIncomingCallImageView.getTop() <= y && y <= mIncomingCallImageView.getBottom())) {
                            // Hide button and set the entire background to red
                            mIncomingCallImageView.setVisibility(View.INVISIBLE);
                            //mMainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed));
                            mMainLayout.setVisibility(View.INVISIBLE);
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
                            mCircleLine.setVisibility(View.INVISIBLE);

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
                    // TODO: Solve the swipe situation
                    Log.i("RINGING", "is currently at: " + String.valueOf(x) + ", " + String.valueOf(y));
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
                if (Build.MANUFACTURER.equalsIgnoreCase("HTC")) { // Unique for HTC
                    if (mAudioManager != null && !mAudioManager.isWiredHeadsetOn()) {
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

                sendOrderedBroadcast(btnDown, enforcedPerm);
                sendOrderedBroadcast(btnUp, enforcedPerm);
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
                } catch (NullPointerException e) { /* Do Nothing */ }
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

    private void requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CALL_PHONE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
            Snackbar.make(mMainLayout, R.string.permission_read_phone_state,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    PERMISSIONS_LIST,
                                    REQUEST_CODE_FOR_GROUP);
                        }
                    })
                    .show();
        } else {
            // Permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(this, PERMISSIONS_LIST, REQUEST_CODE_FOR_GROUP);
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
                                    REQUEST_CODE_FOR_READ_PHONE_STATE);
                        }
                    })
                    .show();
        } else {
            // Read phone state permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_CODE_FOR_READ_PHONE_STATE);
        }
    }

    @SuppressLint("InlinedApi")
    private void requestAnswerPhoneCallsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ANSWER_PHONE_CALLS)) {
            Snackbar.make(mMainLayout, R.string.permission_answer_phone_calls,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ANSWER_PHONE_CALLS},
                                    REQUEST_CODE_FOR_ANSWER_PHONE_CALLS);
                        }
                    })
                    .show();
        } else {
            // ANSWER_PHONE_CALLS permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ANSWER_PHONE_CALLS},
                    REQUEST_CODE_FOR_ANSWER_PHONE_CALLS);
        }
    }

    private void requestCallPhonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CALL_PHONE)) {
            Snackbar.make(mMainLayout, R.string.permission_call_phone,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CALL_PHONE},
                                    REQUEST_CODE_FOR_CALL_PHONE);
                        }
                    })
                    .show();
        } else {
            // CALL_PHONE permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE},
                    REQUEST_CODE_FOR_CALL_PHONE);
        }
    }

    private void requestModifyAudioSettingsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
            Snackbar.make(mMainLayout, R.string.permission_modify_audio_settings,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                                    REQUEST_CODE_FOR_MODIFY_AUDIO_SETTINGS);
                        }
                    })
                    .show();
        } else {
            // MODIFY_AUDIO_SETTINGS permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                    REQUEST_CODE_FOR_MODIFY_AUDIO_SETTINGS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String message = null;

//        switch (requestCode) {
//            case REQUEST_CODE_FOR_READ_PHONE_STATE:
//                message = getResources().getString(R.string.permission_available_read_phone_state);
//                break;
//            case REQUEST_CODE_FOR_ANSWER_PHONE_CALLS:
//                message = getResources().getString(R.string.permission_available_answer_phone_calls);
//                break;
//            case REQUEST_CODE_FOR_CALL_PHONE:
//                message = getResources().getString(R.string.permission_available_call_phone);
//                break;
//            case REQUEST_CODE_FOR_MODIFY_AUDIO_SETTINGS:
//                message = getResources().getString(R.string.permission_available_modify_audio_settings);
//                break;
//            default:
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//                break;
//        }
//
//        if (!TextUtils.isEmpty(message)) {
//            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission has been granted
//                Snackbar.make(mMainLayout, message, Snackbar.LENGTH_SHORT).show();
//            } else {
//                Snackbar.make(mMainLayout, R.string.permissions_not_granted, Snackbar.LENGTH_SHORT).show();
//            }
//        }

        // TODO: Permissions V2
        if (requestCode == REQUEST_CODE_FOR_ANSWER_PHONE_CALLS) {
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ANSWER_PHONE_CALLS permission has been granted
                Snackbar.make(mMainLayout, R.string.permission_available_answer_phone_calls,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mMainLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();

            }
        } else if (requestCode == REQUEST_CODE_FOR_GROUP) {
            // We have requested multiple permissions for contacts, so all of them need to be checked.
            if (PermissionUtil.verifyPermissions(grantResults)) {
                // All required permissions have been granted
                Snackbar.make(mMainLayout, R.string.permissions_available,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                Snackbar.make(mMainLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
