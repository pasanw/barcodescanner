package me.dm7.barcodescanner.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public abstract class BarcodeScannerView extends FrameLayout implements Camera.PreviewCallback {
    protected static CameraHandlerThread mCameraHandlerThread;
    protected CameraWrapper mCameraWrapper;
    protected CameraPreview mPreview;
    protected IViewFinder mViewFinderView;
    protected Rect mFramingRectInPreview;
    protected Boolean mFlashState;
    protected boolean mAutofocusState = true;
    protected boolean mShouldScaleToFill = true;

    public BarcodeScannerView(Context context) {
        super(context);
    }

    public BarcodeScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.BarcodeScannerView,
                0, 0);

        try {
            setShouldScaleToFill(a.getBoolean(R.styleable.BarcodeScannerView_shouldScaleToFill, true));
        } finally {
            a.recycle();
        }
    }

    public final void setupLayout(CameraWrapper cameraWrapper) {
        if (mPreview == null || mViewFinderView == null) {
            removeAllViews();
            mPreview = new CameraPreview(getContext(), cameraWrapper, this);
            mPreview.setShouldScaleToFill(mShouldScaleToFill);
            if (!mShouldScaleToFill) {
                RelativeLayout relativeLayout = new RelativeLayout(getContext());
                relativeLayout.setGravity(Gravity.CENTER);
                relativeLayout.setBackgroundColor(Color.BLACK);
                relativeLayout.addView(mPreview);
                addView(relativeLayout);
            } else {
                addView(mPreview);
            }

            mViewFinderView = createViewFinderView(getContext());
            if (mViewFinderView instanceof View) {
                addView((View) mViewFinderView);
            } else {
                throw new IllegalArgumentException("IViewFinder object returned by " +
                        "'createViewFinderView()' should be instance of android.view.View");
            }
            mViewFinderView.setupViewFinder();
        } else {
            // Re-use the SurfaceView/CameraPreview and ViewFinder
            mPreview.setCamera(cameraWrapper, this);
            mPreview.showCameraPreview();
        }
    }

    /**
     * <p>Method that creates view that represents visual appearance of a barcode scanner</p>
     * <p>Override it to provide your own view for visual appearance of a barcode scanner</p>
     *
     * @param context {@link Context}
     * @return {@link android.view.View} that implements {@link ViewFinderView}
     */
    protected IViewFinder createViewFinderView(Context context) {
        return new ViewFinderView(context);
    }

    public synchronized void startCamera(int cameraId) {
        if(mCameraHandlerThread == null) {
            mCameraHandlerThread = new CameraHandlerThread();
        }
        mCameraHandlerThread.startCamera(cameraId, this);
    }

    public void startCamera() {
        startCamera(CameraUtils.getDefaultCameraId());
    }

    public void stopCamera() {
        stopAndCleanupCameraPreview();
        mCameraHandlerThread.stopCamera(this);
    }

    public void switchCamera(int cameraId) {
        if (mCameraHandlerThread != null) {
            mCameraHandlerThread.stopCamera(this);
        }
        startCamera(cameraId);
    }

    public void setupCameraPreview(CameraWrapper cameraWrapper) {
        mCameraWrapper = cameraWrapper;
        if (mCameraWrapper != null) {
            setupLayout(mCameraWrapper);
            if (mFlashState != null) {
                setFlash(mFlashState);
            }
            setAutoFocus(mAutofocusState);
        }
    }

    public void stopAndCleanupCameraPreview() {
        if(mPreview != null) {
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
        }
    }

    public void stopCameraPreview() {
        if(mPreview != null) {
            mPreview.stopCameraPreview();
        }
    }

    protected void resumeCameraPreview() {
        if(mPreview != null) {
            mPreview.showCameraPreview();
        }
    }

    public synchronized Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if (mFramingRectInPreview == null) {
            Rect framingRect = mViewFinderView.getFramingRect();
            int viewFinderViewWidth = mViewFinderView.getWidth();
            int viewFinderViewHeight = mViewFinderView.getHeight();
            if (framingRect == null || viewFinderViewWidth == 0 || viewFinderViewHeight == 0) {
                return null;
            }

            Rect rect = new Rect(framingRect);

            if(previewWidth < viewFinderViewWidth) {
                rect.left = rect.left * previewWidth / viewFinderViewWidth;
                rect.right = rect.right * previewWidth / viewFinderViewWidth;
            }

            if(previewHeight < viewFinderViewHeight) {
                rect.top = rect.top * previewHeight / viewFinderViewHeight;
                rect.bottom = rect.bottom * previewHeight / viewFinderViewHeight;
            }

            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }

    public void setFlash(boolean flag) {
        mFlashState = flag;
        if(mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {

            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if(flag) {
                if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    public boolean getFlash() {
        if(mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {
            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void toggleFlash() {
        if(mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {
            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        mAutofocusState = state;
        if(mPreview != null) {
            mPreview.setAutoFocus(state);
        }
    }

    public void setShouldScaleToFill(boolean shouldScaleToFill) {
        mShouldScaleToFill = shouldScaleToFill;
    }
}
