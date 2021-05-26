package lu.uni.trux.proxy_double;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native-lib");
    }

    public native String nativeProxy(String s);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelephonyManager tm =  (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        String s = nativeProxy(imei);
        Log.d("Test", "" + s);
    }
}