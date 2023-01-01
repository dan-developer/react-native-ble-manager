package it.innove;


import android.bluetooth.BluetoothAdapter;
import android.os.ParcelUuid;
import android.util.Log;
import com.facebook.react.bridge.*;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import androidx.annotation.NonNull;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;

public class DanScanManager extends ScanManager {

	public DanScanManager(ReactApplicationContext reactContext, BleManager bleManager) {
		super(reactContext, bleManager);
	}

	@Override
	public void stopScan(Callback callback) {
		// update scanSessionId to prevent stopping next scan by running timeout thread
		scanSessionId.incrementAndGet();

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();

		scanner.stopScan(mScanCallback);
		callback.invoke();
	}

    @Override
    public void scan(ReadableArray serviceUUIDs, final int scanSeconds, ReadableMap options,  Callback callback) {
        // Scanning settings
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(2)
                .setMatchMode(1)
                .setNumOfMatches(3)
                .setReportDelay(0)
                .setUseHardwareBatchingIfSupported(false)
                .build();

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(null, settings, mScanCallback);

        if (scanSeconds > 0) {
            Thread thread = new Thread() {
                private int currentScanSession = scanSessionId.incrementAndGet();

                @Override
                public void run() {

                    try {
                        Thread.sleep(scanSeconds * 1000);
                    } catch (InterruptedException ignored) {
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothAdapter btAdapter = getBluetoothAdapter();
                            // check current scan session was not stopped
                            if (scanSessionId.intValue() == currentScanSession) {
                                if(btAdapter.getState() == BluetoothAdapter.STATE_ON) {
                                    scanner.stopScan(mScanCallback);
                                }
                                WritableMap map = Arguments.createMap();
                                bleManager.sendEvent("BleManagerStopScan", map);
                            }
                        }
                    });

                }

            };
            thread.start();
        }
        callback.invoke();
    }

	private ScanCallback mScanCallback = new ScanCallback() {
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.i(bleManager.LOG_TAG, "DiscoverPeripheral: " + result.getDevice().getName());

                    DanPeripheral peripheral = (DanPeripheral) bleManager.getPeripheral(result.getDevice());
                    if (peripheral == null) {
                        peripheral = new DanPeripheral(bleManager.getReactContext(), result);
                    } else {
                        peripheral.updateData(result);
                        peripheral.updateRssi(result.getRssi());
                    }
                    bleManager.savePeripheral(peripheral);

					WritableMap map = peripheral.asWritableMap();
					bleManager.sendEvent("BleManagerDiscoverPeripheral", map);
				}
			});
		}

        public void onBatchScanResults(@NonNull final List<no.nordicsemi.android.support.v18.scanner.ScanResult> results) {
        }

		@Override
		public void onScanFailed(final int errorCode) {
            WritableMap map = Arguments.createMap();
            bleManager.sendEvent("BleManagerStopScan", map);
		}
	};

}
