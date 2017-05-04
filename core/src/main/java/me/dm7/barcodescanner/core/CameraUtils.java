package me.dm7.barcodescanner.core;

import android.hardware.Camera;

import java.util.List;

public class CameraUtils {
    public enum CameraFacing {
        BACK,
        FRONT
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        return getCameraInstance(getDefaultCameraId());
    }

    /** Favor back-facing camera by default. If none exists, fallback to whatever camera is available **/
    public static int getDefaultCameraId() {
        return getCameraId(CameraFacing.BACK);
    }

    public static int getCameraId(CameraFacing facing) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int defaultCameraId = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            defaultCameraId = i;
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing.ordinal()) {
                return i;
            }
        }
        return defaultCameraId;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        /** Since we close the camera on a background thread, we retry to allow some time for the camera
            to be released so that cases like rotation do not result in a failed camera retrieval **/
        for (int numAttempts = 0; numAttempts < 15; numAttempts++) {
            try {
                if (cameraId == -1) {
                    c = Camera.open(); // attempt to get a Camera instance
                } else {
                    c = Camera.open(cameraId); // attempt to get a Camera instance
                }

                if (c != null) {
                    return c;
                }
            } catch (Exception e) {
                // Camera is not available (in use or does not exist)
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return c; // returns null if camera is unavailable
    }

    public static boolean isFlashSupported(Camera camera) {
        /* Credits: Top answer at http://stackoverflow.com/a/19599365/868173 */
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            if (parameters.getFlashMode() == null) {
                return false;
            }

            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes == null || supportedFlashModes.isEmpty() || supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
}