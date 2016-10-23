package ru.unflag.brisa;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BrisaFirebaseMessagingService extends FirebaseMessagingService {

    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getNotification() != null) {

            String message = remoteMessage.getNotification().getBody();
            Log.d(MessagesActivity.LOG_TAG, "Notification: " + message);

            Intent intent = new Intent(MessagesActivity.BROADCAST_ACTION);
            intent.putExtra(MessagesActivity.MESSAGE_TAG, message);

            sendBroadcast(intent);

        }
    }
}
