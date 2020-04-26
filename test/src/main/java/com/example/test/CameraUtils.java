package com.example.test;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class CameraUtils {


    private static final String TAG = "CameraUtils测试: ";

    private MediaRecorder mediaRecorder;
    private Camera camera;
    private boolean isCheck;
    private SurfaceHolder.Callback callback;
    private Context context;
    private SurfaceView surfaceView;
    /***默认视频宽高*/
    private int height = 240, width = 320;

    private MediaCodec mediaCodec;

    public void create(SurfaceView mSurfaceView, Context context) {
        Log.d(TAG, "create: ");
        this.context = context;
        this.surfaceView = mSurfaceView;

        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.setKeepScreenOn(true);
        callback = new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated: ");
                try {
                    camera = Camera.open();
                } catch (Exception e) {
                    Log.d(TAG, "camera**捕捉异常: --" + e.toString());
                }

                getVideoSize();

                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                params.width = width;
                params.height = height;
                surfaceView.setLayoutParams(params);



                //setVideoFrameRate();
                if (mediaRecorder == null) {
                    mediaRecorder = new MediaRecorder();
                } else {
                    mediaRecorder.reset();
                }
            }

            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                Log.d(TAG, "surfaceChanged: ");
                doChange(holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed: ");
                if (camera != null) {
                    camera.release();
                    camera = null;
                }
            }
        };
        surfaceView.getHolder().addCallback(callback);
    }

    private void doChange(SurfaceHolder holder) {
        Log.d(TAG, "doChange: ");
        try {
            //camera.setPreviewDisplay(holder);
            //camera.setDisplayOrientation(0);
            //camera.startPreview();
            camera.setPreviewDisplay(holder);

            int x = getDisplayRotation((Activity) context);

            Log.d(TAG, "doChange: --" + x);

            if (x == 0) {
                camera.setDisplayOrientation(90);
            } else if (x == 90) {
                camera.setDisplayOrientation(0);
            }else if(x == 180){
                camera.setDisplayOrientation(270);

            }else if(x == 270){

                camera.setDisplayOrientation(180);
            }

            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * startRecord 前调用一次，保证为前置摄像头
     */
    public void changeCamera() {
        Log.d(TAG, "changeCamera: ");
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            // 设置为后置
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                camera = Camera.open(i);
                //  todo
                //   setCameraDisplayOrientation((Activity)context,i,camera);

                try {
                    camera.setPreviewDisplay(surfaceView.getHolder());
                    int x = getDisplayRotation((Activity) context);
                    Log.d(TAG, "doChange: --" + x);

                    camera.setDisplayOrientation(x);

                    if (x == 0) {
                        camera.setDisplayOrientation(90);
                    } else if (x == 90) {
                        camera.setDisplayOrientation(0);
                    }else if(x == 180){
                        camera.setDisplayOrientation(270);

                    }else if(x == 270){

                        camera.setDisplayOrientation(180);
                    }
                    //camera.setDisplayOrientation(90);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.startPreview();
                isCheck = true;
                break;
            }
        }

    }

    public void stopRecord() {
        try {
            if (camera != null) {
                camera.lock();
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            if (mediaRecorder != null) {
                mediaRecorder.setOnErrorListener(null);
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                mediaRecorder = null;
                surfaceView = null;

            }


        } catch (Exception e) {
            Log.d(TAG, "stopRecord: " + e.toString());
        }


    }

    public void destroy() {
        try {
            Log.d(TAG, "destroy: ");
            if (mediaRecorder != null && camera != null) {
                mediaRecorder.setOnErrorListener(null);
                mediaRecorder.stop();
                mediaRecorder.release();
                camera.release();
                mediaRecorder = null;
                camera = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "destroy: " + e.toString());
        }


    }

    public String keepVideoPath = "";

    /**
     * 开启预览
     * @param path 保存的路径
     * @param name 录像视频名称(不包含后缀)
     */
    public void startPrepare(String path, String name) {
        camera.unlock();
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        mediaRecorder.setCamera(camera);
        // 设置音视频数据源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // setOutPutFormat
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // set videoEncoder audioEncoder  都放在outputFormat 后面
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(500 * 1024);
        mediaRecorder.setVideoSize(width, height);
        // mediaRecorder.setVideoFrameRate(14);
        // getVideoSize();
        // setAudioEncoder 要 放在setOutputFormat 后面
        // mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // MediaRecorder.AudioEncoder.DEFAULT
        // mediaRecorder.setAudioSamplingRate(16000);
        Log.d(TAG, "startPrepare: - path: --"+path);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        mediaRecorder.setOutputFile(path);
        File file1 = new File(path);
        if (file1.exists()) {
            file1.delete();
        }
        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
        mediaRecorder.setOrientationHint(270);
        try {
            mediaRecorder.prepare();
            // mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * startRecord 里面拉出来
     */
    public void startRecord() {
        mediaRecorder.start();
        Log.d(TAG, "setRecord: camera**--startRecord");
    }

    /***
     * 获取SupportedVideoSizes 控制输出视频width在300到600之间,选择最小的
     *
     * <>出现的异常： 测试6.0 华为ALE-UL00 这里得到的videoSize为null，所以 视频w，h，给了个默认值. 为null原因待查</>
     */
    public void getVideoSize() {
        Log.d(TAG, "getVideoSize: ");
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> videoSize = parameters.getSupportedVideoSizes();
        if (videoSize != null) {
            for (int i = 0; i < videoSize.size(); i++) {
                int width1 = videoSize.get(i).width;
                int height1 = videoSize.get(i).height;
                if (width1 == screenHeight) {
                    if (height1 == screenWidth) {
                        width = height1;
                        height = width1;
                        break;
                    }

                }
                Log.d(TAG, "getVideoSize:----w:-- " + videoSize.get(i).width + "---h:--" + videoSize.get(i).height);
            }

            if (width != 320 && height != 240) {
                for (int i = 0; i < videoSize.size(); i++) {
                    int width1 = videoSize.get(i).width;
                    int height1 = videoSize.get(i).height;
                    if (width1 >= 300 && width1 <= 500) {
                        if (height1 >= 200 && height1 <= 500) {
                            width = width1;
                            height = height1;
                            break;
                        }

                    }
                    Log.d(TAG, "getVideoSize:----w:-- " + videoSize.get(i).width + "---h:--" + videoSize.get(i).height);
                }
            }


        }

        Log.d(TAG, "最终数据 getVideoSize:----w:-- " + width + "---h:--" + height);

    }


    public void setVideoFrameRate() {
        CamcorderProfile cProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_LOW);
        mediaRecorder.setProfile(cProfile);
        //设置录制的视频帧率,注意文档的说明:
        mediaRecorder.setVideoFrameRate(20);


    }


    private int screenWidth, screenHeight;

    public void setScreenSize(int w, int h) {
        this.screenHeight = h;
        this.screenWidth = w;

    }


    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        // See android.hardware.Camera.setCameraDisplayOrientation for
        // documentation.
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getDisplayRotation(activity);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private boolean isPor = false;

    public void setScreen(boolean isPor) {

        this.isPor = isPor;
    }

}
