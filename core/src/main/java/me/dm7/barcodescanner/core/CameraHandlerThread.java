package me.dm7.barcodescanner.core;

import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

// This code is mostly based on the top answer here: http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes
public class CameraHandlerThread extends HandlerThread {
    private final Handler localHandler;

    public CameraHandlerThread() {
        super("CameraHandlerThread");
        start();
        localHandler = new Handler(getLooper());
    }

    public void startCamera(final int cameraId, final BarcodeScannerView mScannerView) {
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                final Camera camera = CameraUtils.getCameraInstance(cameraId);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScannerView.setupCameraPreview(CameraWrapper.getWrapper(camera, cameraId));
                    }
                });
            }
        });
    }

    public void stopCamera(final BarcodeScannerView mScannerView) {
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                mScannerView.stopAndCleanupCameraPreview();
                mScannerView.releaseAndCleanupCamera();
            }
        });
    }
}
