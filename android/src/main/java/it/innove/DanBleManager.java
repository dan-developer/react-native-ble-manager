package it.innove;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.util.Log;
import no.nordicsemi.android.ble.BleManager;

import androidx.annotation.NonNull;

public class DanBleManager extends BleManager {
    private static final String TAG = "ReactNativeBleManager";

    public DanBleManager(@NonNull final Context context) {
        super(context);
    }

    @Override
    public int getMinLogPriority() {
        // Use to return minimal desired logging priority.
        return Log.VERBOSE;
    }

    @Override
    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, message);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new MyGattCallbackImpl();
    }

    private class MyGattCallbackImpl extends BleManagerGattCallback {
        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            // Here get instances of your characteristics.
            // Return false if a required service has not been discovered.
            return true;
        }

        @Override
        protected void initialize() {
            // Initialize your device.
            // This means e.g. enabling notifications, setting notification callbacks,
            // sometimes writing something to some Control Point.
            // Kotlin projects should not use suspend methods here, which require a scope.
            requestMtu(245)
                    .enqueue();
        }

        @Override
        protected void onServicesInvalidated() {
            // This method is called when the services get invalidated, i.e. when the device
            // disconnects.
            // References to characteristics should be nullified here.
        }
    }
}
