package com.aboutfuture.capturecalls;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.service.notification.NotificationListenerService;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@SuppressLint("OverrideAbstract")
public class NotificationReceiverService extends NotificationListenerService {
    public NotificationReceiverService() {
    }
}
