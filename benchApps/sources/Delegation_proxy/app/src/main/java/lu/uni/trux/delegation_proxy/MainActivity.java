package lu.uni.trux.delegation_proxy;

import android.app.Activity;
import android.os.Bundle;

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