package me.dm7.barcodescanner.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

// This code is mostly based on the top answer here: http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes
public class CameraHandlerThread extends HandlerThread {
    private static final NonReentrantLock lock = new NonReentrantLock();
    private static CameraWrapper cameraWrapper;
    private final Handler localHandler;
    private final Handler mainHandler;

    public CameraHandlerThread() {
        super("CameraHandlerThread");
        start();
        localHandler = new Handler(getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startCamera(final int cameraId, final BarcodeScannerView mScannerView) {
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    Log.e("CMDBG: "+Thread.currentThread().getId(), "startCamera: Lock acquired");
                    cameraWrapper = CameraWrapper.getWrapper(CameraUtils.getCameraInstance(cameraId), cameraId);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScannerView.setupCameraPreview(cameraWrapper);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void stopCamera(final BarcodeScannerView mScannerView) {
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                if (cameraWrapper != null && cameraWrapper.mCamera != null) {
                    cameraWrapper.mCamera.release();
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScannerView.stopAndCleanupCameraPreview();
                    }
                });

                lock.unlock();
                Log.e("CMDBG: "+Thread.currentThread().getId(), "stopCamera: Lock released");
            }
        });
    }
}
