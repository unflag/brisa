package ru.unflag.brisa;

import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class BrisaFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedAuthToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(MessagesActivity.LOG_TAG, "refreshedAuthToken: " + refreshedAuthToken);
    }
}
