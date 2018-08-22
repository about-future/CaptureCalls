package com.aboutfuture.capturecalls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && !TextUtils.equals(intent.getAction(),"android.intent.action.NEW_OUTGOING_CALL")) {
            String state = extras.getString(TelephonyManager.EXTRA_STATE);

            // If the phone is ringing or the call was answered
            if (TextUtils.equals(state, TelephonyManager.EXTRA_STATE_RINGING)) {
                // Get the calling number
                String phoneNumber = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

                // Check is the number is not null and has more than 4 digits
                if (!TextUtils.isEmpty(phoneNumber) && phoneNumber.length() > 4) {
                    // Subtract the first part of the phone number, without the last 4 digits
                    String subNumber = phoneNumber.substring(0, phoneNumber.length() - 4);
                    int length = subNumber.length();

                    String starReplacement = "";
                    int i = 0;
                    // Generate a string composed of "*"(star symbols) as long as subNumber is
                    while (i < length) {
                        starReplacement = starReplacement.concat("*");
                        i++;
                    }

                    // Replace the subNumber with the starReplacement
                    final String hiddenNumber = phoneNumber.replace(subNumber, starReplacement);

                    // After one second since the incoming call was intercepted, open MainActivity
                    // and pass the hidden number.
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
            }
        }
    }
}
