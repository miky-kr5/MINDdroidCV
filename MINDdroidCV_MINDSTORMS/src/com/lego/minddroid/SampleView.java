package com.lego.minddroid;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class SampleView extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private static final String TAG = "CAMERA_PREVIEW";
	private int mFrameWidth;
	private int mFrameHeight;
	protected CameraGLRenderer mGLRender;
	protected MINDdroidCV mActivity;
	protected double[]buffer;
	protected int left, right;

	public SampleView(Context context, CameraGLRenderer renderer, MINDdroidCV uiActivity) {
		super(context);
		mCamera = Camera.open();
		mGLRender = renderer;
		mActivity = uiActivity;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		renderer.setMActivity(uiActivity);

		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the preview.
		try {

			/**/
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public int getFrameWidth() {
		return mFrameWidth;
	}

	public int getFrameHeight() {
		return mFrameHeight;
	}


	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		try {
			if (mCamera!=null) {
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();        // release the camera for other applications
				mCamera = null;
			}
		} catch (Exception e) {

		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		mFrameWidth = w;
		mFrameHeight = h;

		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			Camera.Parameters p = mCamera.getParameters();
			p.setPreviewSize(240, 160);
			mCamera.setParameters(p);
			mGLRender.w = w;
			mGLRender.h = h;
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
			mCamera.setPreviewCallback(mGLRender);
		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}
}
