package com.company.cpp.hellocv;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.company.cpp.hellocv.R.id.imageView;

public class MainActivity extends AppCompatActivity {

    private long start;
    private long end;
    private long t;

    private Bitmap mBitmap;
    private Bitmap mBitmapGray;
    public long exposureDuration =1000000000l / 30;

    public NativeClass testNativeClass = new NativeClass();
    public ImageToMat imageToMat = new ImageToMat();


    //    private  int FPS
    private static final String TAG = "Camera2VideoImageActivi";

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private Handler handler;
    private String mCameraId;
    private Size mImageSize;
    private Mat mRgba;
    private Mat mGray;

    static {
        if (OpenCVLoader.initDebug()){
            Log.i(TAG,"OpenCV loaded successfully");
        }else{
            Log.i(TAG,"OpenCV not loaded");
        }
    }

    static {
        System.loadLibrary("MyOpencvLibs");
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");

        //DRS 20160822b - Added 1
        System.loadLibrary("opencv_java3");
    }

    public TextView tvNative;
    private ImageButton mStillImageButton;

    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_video_image);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tvNative = (TextView) findViewById(R.id.textView);

        mStillImageButton = (ImageButton) findViewById(R.id.cameraImageButton2);
        mStillImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double temp = 16000;
                if (mCaptureRequestBuilder == null) {
                    return;
                }
                long ae = (long)(1000000000l/16000);
                mCaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, ae);
                exposureDuration = ae;
                updatePreview();

            }
        });

        mImageView = (ImageView) findViewById(imageView);

        setupCamera(600, 800);
        connectCamera();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }

    private ImageReader mImageReader;
    protected ImageSaver mImageSaver;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {

                    mImageSaver = null;
                    mImageSaver = new ImageSaver(reader.acquireNextImage());
                    handler.post(mImageSaver);

                }
            };
    private class ImageSaver implements Runnable {

        private final Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            try {
                start = System.currentTimeMillis();

                Mat mYuvMat = imageToMat.convertImageToMatFormat(mImage);
                Mat bgrMat = new Mat(mImage.getHeight(), mImage.getWidth(), CvType.CV_8UC4);
                mImage.close();
                Imgproc.cvtColor(mYuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_I420);
                Mat rgbaMatOut = new Mat();
                Imgproc.cvtColor(bgrMat, rgbaMatOut, Imgproc.COLOR_BGR2RGBA, 0);
                final Bitmap bitmap = Bitmap.createBitmap(bgrMat.cols(), bgrMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgbaMatOut, bitmap);

                if ( exposureDuration == 1000000000l/16000) {

                    Utils.bitmapToMat(bitmap, mRgba);
                    NativeClass.lightDetection(mRgba.getNativeObjAddr());
                    Utils.matToBitmap(mRgba, bitmap);
                }

                tvNative.setText(""+testNativeClass.getJniStringBytes());
                System.out.print("text data: "+testNativeClass.getJniStringBytes()+ "\n");

                mImageView.setImageBitmap(bitmap);
                end = System.currentTimeMillis();
                mImageView.setRotation(90);
                t = end - start;
                System.out.println("Total time:" + t + "milisecond" );
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;

    public CaptureRequest.Builder mCaptureRequestBuilder;

    private ImageButton mRecordImageButton;

    private boolean mIsRecording = false;
    private boolean mIsTimelapse = false;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum( (long)(lhs.getWidth() * lhs.getHeight()) -
                    (long)(rhs.getWidth() * rhs.getHeight()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mImageView.isActivated()) {
            setupCamera(600, 800);
            connectCamera();
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(mImageSaver);
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    public void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;

                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                System.out.println("do rong hinh:" + width );
                System.out.println("do dai hinh:" + height );

                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.YUV_420_888), rotatedWidth, rotatedHeight);

                //////////
                mBitmap = Bitmap.createBitmap(mImageSize.getWidth(), mImageSize.getHeight(), Bitmap.Config.ARGB_8888);
                mBitmapGray = Bitmap.createBitmap(mImageSize.getWidth(), mImageSize.getHeight(), Bitmap.Config.ARGB_8888);
                mRgba = new Mat(mImageSize.getHeight(), mImageSize.getWidth(), CvType.CV_8UC3);
                mGray = new Mat(mImageSize.getHeight(), mImageSize.getWidth(), CvType.CV_8UC4);

                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.YUV_420_888, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,handler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {android.Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                    }, REQUEST_CAMERA_PERMISSION_RESULT);
                }

            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void setupRequest( CaptureRequest.Builder request)
    {
        request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureDuration);
        request.set(CaptureRequest.SENSOR_FRAME_DURATION, 1000000000l /20);

    }
    private void startPreview() {

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            setupRequest(mCaptureRequestBuilder);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);

            mCameraDevice.createCaptureSession(Arrays.asList( mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigured: startPreview");
                            mPreviewCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startPreview");

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */

    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;

        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");

        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
        handler = new Handler(Looper.getMainLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
            handler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }
}
