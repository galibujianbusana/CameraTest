package com.example.camera2_2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.sembozdemir.permissionskt.askPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    var mCameraId: String? = null
    var mCameraIdFront: String? = null
    var mPreviewSize: Size? = null

    var width: Int = 0
    var height: Int = 0

    //camera2
    var mCameraDevice: CameraDevice? = null
    var mPreviewBuilder: CaptureRequest.Builder? = null
    var mCaptureRequest: CaptureRequest? = null
    var mPreviewSession: CameraCaptureSession? = null
    var characteristics: CameraCharacteristics? = null
    var mImageReader: ImageReader? = null
    var videoSavePath: String? = null

    //
    var mCameraThread: HandlerThread? = null
    var mCameraHandler: Handler? = null
    var mMediaRecorder: MediaRecorder? = null

    var zoom_leavel = 0
    var zoom: Rect? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) {
            onGranted {

            }
            onDenied { permissions ->
                permissions.forEach {
                    when (it) {
                    }
                }
            }
        }
        initTextureView()
        initView()
    }

    private fun initTextureView() {
        mCameraThread = HandlerThread("CameraThread")
        mCameraThread!!.start()
        mCameraHandler = Handler(mCameraThread!!.looper)
        textureView.surfaceTextureListener = this
    }

    private fun initView() {
        btnStop.setOnClickListener {
            stopMediaRecorder()
        }
        btnStart.setOnClickListener {
            prepareMediaRecorder()
            startMediaRecorder()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        this.width = width
        this.height = height
        setUpCamera(width, height)
        openCamera(mCameraId!!)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        TODO("Not yet implemented")
    }

    private fun setUpCamera(width: Int, height: Int) {
        var mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraId = mCameraManager.cameraIdList[0]
        mCameraIdFront = mCameraManager.cameraIdList[1]
        characteristics = mCameraManager.getCameraCharacteristics(mCameraId!!)
        var map = characteristics!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        mPreviewSize = Camera2Util.getMinPreSize(
            map!!.getOutputSizes(SurfaceTexture::class.java),
            width,
            height,
            Camera2Config.PREVIEW_MAX_HEIGHT
        )
        mMediaRecorder = MediaRecorder()

    }

    private fun openCamera(cameraId: String) {
        var manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.openCamera(cameraId, mStateCallback, null)
    }

    private var mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            mCameraDevice = null
        }

    }


    // 初始录像参数
    var isCameraFront = false
    private fun setUpMediaRecorder() {
        try {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            // 这里有点投机取巧的方式，不过证明方法也是不错的
            // 录制出来10S的视频，大概1.2M，清晰度不错，而且避免了因为手动设置参数导致无法录制的情况
            // 手机一般都有这个格式CamcorderProfile.QUALITY_480P,因为单单录制480P的视频还是很大的，所以我们在手动根据预览尺寸配置一下videoBitRate,值越高越大
            // QUALITY_QVGA清晰度一般，不过视频很小，一般10S才几百K
            // 判断有没有这个手机有没有这个参数
            if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
                val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
                profile.videoBitRate = mPreviewSize!!.width * mPreviewSize!!.height
                mMediaRecorder!!.setProfile(profile)
                var obj = Surface(textureView.surfaceTexture)
                mMediaRecorder!!.setPreviewDisplay(obj)
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
                val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
                profile.videoBitRate = mPreviewSize!!.width * mPreviewSize!!.height
                mMediaRecorder!!.setProfile(profile)
                var obj = Surface(textureView.surfaceTexture)
                mMediaRecorder!!.setPreviewDisplay(obj)
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA)) {
                mMediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA))
                var obj = Surface(textureView.surfaceTexture)
                mMediaRecorder!!.setPreviewDisplay(obj)
            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF)) {
                mMediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF))
                var obj = Surface(textureView.surfaceTexture)
                mMediaRecorder!!.setPreviewDisplay(obj)
            } else {
                mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mMediaRecorder!!.setVideoEncodingBitRate(10000000)
                mMediaRecorder!!.setVideoFrameRate(30)
                mMediaRecorder!!.setVideoEncodingBitRate(2500000)
                mMediaRecorder!!.setVideoFrameRate(20)
                mMediaRecorder!!.setVideoSize(mPreviewSize!!.width, mPreviewSize!!.height)
            }

            //判断有没有配置过视频地址了
            Camera2Util.createSavePath(Camera2Config.PATH_SAVE_VIDEO) //判断有没有这个文件夹，没有的话需要创建
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            videoSavePath =
                Camera2Config.PATH_SAVE_VIDEO + "VIDEO_" + timeStamp + ".mp4"
            mMediaRecorder!!.setOutputFile(videoSavePath)

            //判断是不是前置摄像头,是的话需要旋转对应的角度
            if (isCameraFront) {
                mMediaRecorder!!.setOrientationHint(270)
            } else {
                mMediaRecorder!!.setOrientationHint(90)
            }
            mMediaRecorder!!.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startMediaRecorder() {
        mMediaRecorder!!.start()

    }

    private fun prepareMediaRecorder() {

        closePreviewSession()
        setUpMediaRecorder()
        var texture = textureView.surfaceTexture
        texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        var surfaces: MutableList<Surface> = ArrayList()
        var previewSurface = Surface(texture)
        surfaces.add(previewSurface)
        mPreviewBuilder!!.addTarget(previewSurface)
        var recorderSurface = mMediaRecorder!!.surface
        surfaces.add(recorderSurface)

        mPreviewBuilder!!.addTarget(recorderSurface)

        mCameraDevice!!.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }

                override fun onConfigured(session: CameraCaptureSession) {
                    mCaptureRequest = mPreviewBuilder!!.build()
                    mPreviewSession = session
                    mPreviewSession!!.setRepeatingRequest(mCaptureRequest!!, null, mCameraHandler)
                }

            },
            null
        )
    }

    private fun stopMediaRecorder() {

        mMediaRecorder!!.stop()
        mMediaRecorder!!.reset()
        showResetCameraLayout()
    }


    private fun showResetCameraLayout() {
        resetCamera()
    }


    /**
     * 清除操作
     */
    private fun onFinishCapture() {
        try {
            if (mPreviewSession != null) {
                mPreviewSession!!.close()
                mPreviewSession = null
            }
            if (mCameraDevice != null) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (mImageReader != null) {
                mImageReader!!.close()
                mImageReader = null
            }
            if (mMediaRecorder != null) {
                mMediaRecorder!!.release()
                mMediaRecorder = null
            }
            if (mCameraHandler != null) {
                mCameraHandler!!.removeCallbacksAndMessages(null)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    //清除预览Session
    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }

    //重新配置打开相机
    private fun resetCamera() {
        if (TextUtils.isEmpty(mCameraId)) {
            return
        }
        if (mCameraDevice != null) {
            mCameraDevice!!.close()
        }
        setUpCamera(width, height)
        openCamera(mCameraId!!)
    }


    override fun onResume() {
        super.onResume()
        //从FinishActivity退回来的时候默认重置为初始状态，因为有些机型从不可见到可见不会执行onSurfaceTextureAvailable，像有些一加手机
        //所以也可以在这里在进行setupCamera()和openCamera()这两个方法
        //每次开启预览缩放重置为正常状态
        if (zoom != null) {
            zoom!!.setEmpty()
            //zoom_level = 0
        }

        //每次开启预览默认是后置摄像头
        isCameraFront = false
    }

    override fun onPause() {
        onFinishCapture() //释放资源
        super.onPause()
    }
}
