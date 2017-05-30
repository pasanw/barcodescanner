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
        if (getNumberOfCameras() == 0) {
            return -1;
        }

        return getCameraId((getNumberOfFacingCameras(CameraFacing.BACK) != 0) ? CameraFacing.BACK : CameraFacing.FRONT);
    }

    /** Returns the first camera for a given direction. If a camera does not exist for that direction, -1 is returned **/
    public static int getCameraId(CameraFacing facing) {
        int numberOfCameras = getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing.ordinal()) {
                return i;
            }
        }
        return -1;
    }

    public static int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public static int getNumberOfFacingCameras(CameraFacing facing) {
        int numberOfCameras = getNumberOfCameras();
        int numberOfFacingCameras = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if ((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && facing == CameraFacing.BACK)
                    || (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && facing == CameraFacing.FRONT)) {
                numberOfFacingCameras++;
            }
        }
        return numberOfFacingCameras;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            if(cameraId == -1) {
                c = Camera.open(); // attempt to get a Camera instance
            } else {
                c = Camera.open(cameraId); // attempt to get a Camera instance
            }
        }
        catch (Exception e) {
            // Camera is not available (in use or does not exist)
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