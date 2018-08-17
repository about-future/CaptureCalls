package com.aboutfuture.capturecalls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String state = extras.getString(TelephonyManager.EXTRA_STATE);
            Log.w("MY_DEBUG_TAG", state);
            if (TextUtils.equals(state, TelephonyManager.EXTRA_STATE_RINGING) || TextUtils.equals(state, TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                String phoneNumber = extras
                        .getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.w("MY_DEBUG_TAG", phoneNumber);

                if (phoneNumber != null && phoneNumber.length() > 4) {
                    String subNumber = phoneNumber.substring(0, phoneNumber.length() - 4);
                    int length = subNumber.length();
                    String starReplacement = "";
                    int i = 0;
                    while (i < length) {
                        starReplacement = starReplacement.concat("*");
                        i++;
                    }

                    final String hiddenNumber = phoneNumber.replace(subNumber, starReplacement);

                    Log.v("Calling Number:", hiddenNumber);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent mainIntent = new Intent(context, MainActivity.class);
                            mainIntent.putExtra(context.getString(R.string.phone_number_key), hiddenNumber);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(mainIntent);
                        }
                    }, 500);
                }
//            } else {
//                // No call or call ended, so window should close
            }
        }
    }

//    @Override
//    public void onReceive(Context context, Intent intent) {
//        //String action = intent.getAction();
//
//        if (intent.getExtras() != null) {
//            String state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
//            if (TextUtils.equals(state, TelephonyManager.EXTRA_STATE_RINGING)) {
//                String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
//
//                //if (!TextUtils.isEmpty(number) && number.length() > 4) {
//                    String subNumber = number.substring(0, number.length() - 4);
//                    int length = subNumber.length();
//                    String starReplacement = "";
//                    int i = 0;
//                    while (i < length) {
//                        starReplacement = starReplacement.concat("*");
//                    }
//
//                    number = number.replace(subNumber, starReplacement);
//
//                    Log.v("Calling Number:", number);
//                //}
//
////                Intent mainIntent = new Intent(context, MainActivity.class);
////                mainIntent.putExtra(context.getString(R.string.phone_number_key), number);
////                context.startService(mainIntent);
//
//                //state = TelephonyManager.CALL_STATE_RINGING;
//            } else if (TextUtils.equals(state, TelephonyManager.EXTRA_STATE_IDLE)) {
//                // Close activity
//                Toast.makeText(context, "No call or call ended!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
}
