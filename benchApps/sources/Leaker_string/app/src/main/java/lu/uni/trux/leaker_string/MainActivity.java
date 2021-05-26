package lu.uni.trux.leaker_string;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native-lib");
    }

    public native void nativeLeaker(String s);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelephonyManager tm =  (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        nativeLeaker(imei);

    }
}