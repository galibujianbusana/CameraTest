package com.example.camera2_1;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 利用Camera2实现点击拍照，长按录像
 * <p>
 * 联系方式： 471497226@qq.com
 */
public class Camera2RecordActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    
    //views
    private TextureView mTextureView;

    //拍照方向
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    //constant
    private static final String TAG = "Camera2RecordActivity";
    private String mCameraId;//后置摄像头ID
    private String mCameraIdFront;//前置摄像头ID
    private Size mPreviewSize;//预览的Size
    private int width;//TextureView的宽
    private int height;//TextureView的高
    private boolean isCameraFront = false;//当前是否是前置摄像头

    //Camera2
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mPreviewSession;
    private CameraCharacteristics characteristics;
    private ImageReader mImageReader;
    private String videoSavePath;//视频保存路径
    //handler
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    //录像
    private MediaRecorder mMediaRecorder;


    int zoom_level = 0;
    Rect zoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏模式
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //透明导航栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        setContentView(R.layout.activity_camera2_record);

        requestPermission();
        initViews();
        initTextureView();
    }


    //初始化TextureView
    private void initTextureView() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        mTextureView.setSurfaceTextureListener(this);
    }

    //初始化视图控件
    private void initViews() {
        mTextureView = findViewById(R.id.textureView);
        //触摸事件
        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareMediaRecorder();
                startMediaRecorder();//开
            }
        });
        findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMediaRecorder();

            }
        });
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        //当SurefaceTexture可用的时候，设置相机参数并打开相机
        Log.d(TAG, "onSurfaceTextureAvailable: ");
        this.width = width;
        this.height = height;
        setupCamera(width, height);//配置相机参数
        openCamera(mCameraId);//打开相机
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: ");
        // configureTransform(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureDestroyed: ");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureUpdated: ");
    }


    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //0表示后置摄像头,1表示前置摄像头
            mCameraId = manager.getCameraIdList()[0];
            mCameraIdFront = manager.getCameraIdList()[1];
            //前置摄像头和后置摄像头的参数属性不同，所以这里要做下判断
            if (isCameraFront) {
                characteristics = manager.getCameraCharacteristics(mCameraIdFront);
            } else {
                characteristics = manager.getCameraCharacteristics(mCameraId);
            }
            //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //选择预览尺寸
            mPreviewSize = Camera2Util.getMinPreSize(map.getOutputSizes(SurfaceTexture.class), width, height, Camera2Config.PREVIEW_MAX_HEIGHT);
            Log.e(TAG, mPreviewSize.getWidth() + "----" + mPreviewSize.getHeight());
            Log.e(TAG, height + "----" + width);
            // configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCamera(String CameraId) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //检查权限
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            manager.openCamera(CameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            if (null != mTextureView) {
               // configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };


    // 初始录像参数
    private void setUpMediaRecorder() {
        try {
            Log.e("daasddasd", "setUpMediaRecorder");
            mMediaRecorder.reset();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            // 这里有点投机取巧的方式，不过证明方法也是不错的
            // 录制出来10S的视频，大概1.2M，清晰度不错，而且避免了因为手动设置参数导致无法录制的情况
            // 手机一般都有这个格式CamcorderProfile.QUALITY_480P,因为单单录制480P的视频还是很大的，所以我们在手动根据预览尺寸配置一下videoBitRate,值越高越大
            // QUALITY_QVGA清晰度一般，不过视频很小，一般10S才几百K
            // 判断有没有这个手机有没有这个参数
            if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
                CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
                profile.videoBitRate = mPreviewSize.getWidth() * mPreviewSize.getHeight();
                mMediaRecorder.setProfile(profile);
                mMediaRecorder.setPreviewDisplay(new Surface(mTextureView.getSurfaceTexture()));
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
                CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                profile.videoBitRate = mPreviewSize.getWidth() * mPreviewSize.getHeight();

                mMediaRecorder.setProfile(profile);
                mMediaRecorder.setPreviewDisplay(new Surface(mTextureView.getSurfaceTexture()));
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA)) {
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));
                mMediaRecorder.setPreviewDisplay(new Surface(mTextureView.getSurfaceTexture()));
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF)) {
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF));
                mMediaRecorder.setPreviewDisplay(new Surface(mTextureView.getSurfaceTexture()));
            } else {
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mMediaRecorder.setVideoEncodingBitRate(10000000);
                mMediaRecorder.setVideoFrameRate(30);
                mMediaRecorder.setVideoEncodingBitRate(2500000);
                mMediaRecorder.setVideoFrameRate(20);
                mMediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }

            //判断有没有配置过视频地址了
            Camera2Util.createSavePath(Camera2Config.PATH_SAVE_VIDEO);//判断有没有这个文件夹，没有的话需要创建
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            videoSavePath = Camera2Config.PATH_SAVE_VIDEO + "VIDEO_" + timeStamp + ".mp4";
            mMediaRecorder.setOutputFile(videoSavePath);

            //判断是不是前置摄像头,是的话需要旋转对应的角度
            if (isCameraFront) {
                mMediaRecorder.setOrientationHint(270);
            } else {
                mMediaRecorder.setOrientationHint(90);
            }
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //预览录像
    private void prepareMediaRecorder() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
           mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        //创建捕获请求
                        mCaptureRequest = mPreviewBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("dasdasdasdas", "捕获的异常2" + e.toString());
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(Camera2RecordActivity.this, "onConfigureFailed", Toast.LENGTH_SHORT).show();
                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //开始录像
    private void startMediaRecorder() {
        try {
            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //停止录像
    private void stopMediaRecorder() {
        if (TextUtils.isEmpty(videoSavePath)) {
            return;
        }
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            //录制完成后重置录制界面
            showResetCameraLayout();
        } catch (Exception e) {
            Toast.makeText(this, "录制时间过短", Toast.LENGTH_LONG).show();
            showResetCameraLayout();
        }

    }


    public void showResetCameraLayout() {
        resetCamera();
    }



    /**
     *清除操作
     */
    public void onFinishCapture() {
        try {
            if (mPreviewSession != null) {
                mPreviewSession.close();
                mPreviewSession = null;
            }

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }

            if (mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

            if (mCameraHandler != null) {
                mCameraHandler.removeCallbacksAndMessages(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("adsdadadad", e.toString() + "onFinish2()");
        }
    }


    //清除预览Session
    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    //重新配置打开相机
    public void resetCamera() {
        if (TextUtils.isEmpty(mCameraId)) {
            return;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
        }

        setupCamera(width, height);
        openCamera(mCameraId);
    }



    @Override
    protected void onResume() {
        super.onResume();
        //从FinishActivity退回来的时候默认重置为初始状态，因为有些机型从不可见到可见不会执行onSurfaceTextureAvailable，像有些一加手机
        //所以也可以在这里在进行setupCamera()和openCamera()这两个方法
        //每次开启预览缩放重置为正常状态
        if (zoom != null) {
            zoom.setEmpty();
            zoom_level = 0;
        }

        //每次开启预览默认是后置摄像头
        isCameraFront = false;
    }

    @Override
    protected void onPause() {
        onFinishCapture();//释放资源
        super.onPause();
    }


    private void requestPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.Group.STORAGE, Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                    .onGranted(data -> runOnUiThread(() -> {
                    }))
                    .onDenied(data -> {
                        if (AndPermission.hasAlwaysDeniedPermission(Camera2RecordActivity.this, data)) {
                            AndPermission.permissionSetting(Camera2RecordActivity.this).execute();
                            return;
                        }
                    })
                    .start();
        }
    }

}
