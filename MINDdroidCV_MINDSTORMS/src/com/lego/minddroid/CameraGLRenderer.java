package com.lego.minddroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;

public class CameraGLRenderer implements Renderer, PreviewCallback{
	private static final String TAG = "CAMERA_RENDERER";
	Quad t;
	int tex;
	int[] glCameraFrame = null;
	int[] glTexture = null;
	byte[] frame = null;
	int[] cameraTexture;
	public int w,h;
	protected MINDdroidCV mActivity;
	protected double[]buffer;
	protected int left, right;
	private Handler handler = new Handler();

	static {
		System.loadLibrary("mixed_sample");
	}

	public CameraGLRenderer(){
		handler.removeCallbacks(FrameProcessor);
		handler.postDelayed(FrameProcessor, 100);

		w = 0; h = 0;
		t = new Quad();

		frame = new byte[1];
		buffer = new double[10];
		glCameraFrame = new int[240*160];
		glTexture = new int[256*256];
		for(int i = 0; i < 256; ++i){
			for(int j = 0; j < 256; ++j){
				glTexture[i*256 + j] = 0;
			}
		}
	}

	public native void FindLight(int width, int height, byte yuv[], int[] rgba,double[] array);

	public void onDrawFrame( GL10 gl ) {
		gl.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
		gl.glClear( GL10.GL_COLOR_BUFFER_BIT );

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		bindCameraTexture(gl);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, t.vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, t.texBuffer);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

		gl.glFinish();
	}

	public void setMActivity (MINDdroidCV activity){
		mActivity = activity;
	}

	public void onSurfaceChanged( GL10 gl, int width, int height ) {
		gl.glViewport( 0, 0, width, height );
	}

	public void onSurfaceCreated( GL10 gl, EGLConfig config ) {
		gl.glEnable(GL10.GL_TEXTURE_2D);
	}

	void bindCameraTexture(GL10 gl) {
		if (cameraTexture==null)
			cameraTexture=new int[1];
		else
			gl.glDeleteTextures(1, cameraTexture, 0);

		gl.glGenTextures(1, cameraTexture, 0);
		int tex = cameraTexture[0];
		gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
		synchronized (glCameraFrame) {
			int orgRow = 0;
			int destRow = 0;
			for(int i = 0; i < 160; ++i){
				System.arraycopy(glCameraFrame, orgRow, glTexture, destRow, 240);
				orgRow += 240;
				destRow += 256;
			}
			gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, 256, 256, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(glTexture));
		}
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
	}	

	public void onPreviewFrame(byte[] new_frame, Camera camera){
		w = camera.getParameters().getPreviewSize().width;
		h = camera.getParameters().getPreviewSize().height;

		synchronized(frame){
			frame = new_frame.clone();
		}
	}

	private void processFrame(byte[] data) {
		synchronized (glCameraFrame) {
			FindLight(w, h, data, glCameraFrame, buffer);
		}
	}

	void calculateMove() {
		// buffer[1] holds the light direction info if the phone is in landscape format
		// small values -> turn left
		// large values -> turn right
		// in portrait mode buffer[2] should be used 
		// large values -> turn right 
		// small values -> turn left
		if(mActivity.isConnected()){
			mActivity.sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_LIGHT_SENSOR, 0, 0);
			mActivity.sendBTCmessage(40, BTCommunicator.GET_TOUCH_SENSOR, 0, 0);
		}
		if(mActivity.getLightSensorReading() > 600 || mActivity.getTouchSensorReading() > 0){
			left = 0;
			right = 0;
		}else{
			if( buffer[0] > 100 ) { // light is visible
				int forwardSpeed = 50;
				double upScale = 40; 
				double direction = (buffer[1] - 240/2)/160;
				//double direction = -1.0 * (buffer[2] - w/2)/h;
				right = (int)(upScale * direction) + forwardSpeed;
				left = (int)(-1.0 * upScale * direction) + forwardSpeed;
			} else {
				left = 0;
				right = 0;
			}
			left = Math.min(Math.max(left,0),100);
			right = Math.min(Math.max(right,0),100);
		}

		mActivity.updateMotorControl(left,right);
	}

	private Runnable FrameProcessor = new Runnable(){
		public void run(){
			if(mActivity.isConnected()){
				synchronized(frame){
					processFrame(frame);
				}
				calculateMove();
			}
			if(handler != null)
				handler.postDelayed(this, 100);
		}
	};
}

class Quad {
	public FloatBuffer vertexBuffer, texBuffer;

	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	float triangleCoords[] = { // in counterclockwise order:
			-1.0f, -1.0f, 0.0f,   // top
			1.0f, -1.0f, 0.0f,   // bottom left
			-1.0f, 1.0f, 0.0f,   // bottom right
			1.0f, 1.0f, 0.0f,
	};

	float texCoords[] = {
			0.0f, 0.625f,
			0.9375f, 0.625f,
			0.0f, 0.0f,
			0.9375f, 0.0f
	};

	public Quad() {
		// initialize vertex byte buffer for shape coordinates
		ByteBuffer bb = ByteBuffer.allocateDirect(
				// (number of coordinate values * 4 bytes per float)
				triangleCoords.length * 4);
		// use the device hardware's native byte order
		bb.order(ByteOrder.nativeOrder());

		// create a floating point buffer from the ByteBuffer
		vertexBuffer = bb.asFloatBuffer();
		// add the coordinates to the FloatBuffer
		vertexBuffer.put(triangleCoords);
		// set the buffer to read the first coordinate
		vertexBuffer.position(0);

		ByteBuffer bb2 = ByteBuffer.allocateDirect(
				// (number of coordinate values * 4 bytes per float)
				texCoords.length * 4);
		// use the device hardware's native byte order
		bb2.order(ByteOrder.nativeOrder());

		// create a floating point buffer from the ByteBuffer
		texBuffer = bb2.asFloatBuffer();
		// add the coordinates to the FloatBuffer
		texBuffer.put(texCoords);
		// set the buffer to read the first coordinate
		texBuffer.position(0);
	}
}

