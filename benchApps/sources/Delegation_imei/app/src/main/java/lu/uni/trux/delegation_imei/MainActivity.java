package lu.uni.trux.delegation_imei;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native-lib");
    }

    public native void nativeDelegation();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nativeDelegation();
    }
}