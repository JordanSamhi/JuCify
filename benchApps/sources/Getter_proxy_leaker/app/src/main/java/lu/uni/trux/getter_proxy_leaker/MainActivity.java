package lu.uni.trux.getter_proxy_leaker;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native-lib");
    }

    public native String nativeGetImei(TelephonyManager manager);
    public native String nativeProxy(String s);
    public native void nativeLeaker(String s);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelephonyManager tm =  (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = nativeGetImei(tm);
        String s = nativeProxy(imei);
        nativeLeaker(s);
    }
}