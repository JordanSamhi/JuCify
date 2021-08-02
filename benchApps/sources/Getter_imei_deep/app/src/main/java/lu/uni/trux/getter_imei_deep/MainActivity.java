package lu.uni.trux.getter_imei_deep;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native-lib");
    }

    public native String nativeGetImei(TelephonyManager manager);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelephonyManager tm =  (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = nativeGetImei(tm);
        Log.d("IMEI",  "" + imei);

    }
}