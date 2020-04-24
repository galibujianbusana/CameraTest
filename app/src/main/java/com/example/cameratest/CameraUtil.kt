package com.example.cameratest

import android.app.Activity
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.open
import android.media.CamcorderProfile
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import java.io.File

/**
 * @ClassName CameraUtil
 * @Description TODO
 * @Date 2020/4/24 13:51
 */
class CameraUtil {

    companion object {
        const val TAG = "CameraUtil"
        fun log(string: String){
            Log.d(TAG,string)
        }
    }

    var mediaRecorder: MediaRecorder? = null
    var camera: Camera? = null
    var context: Activity? = null
    var isCheck = false
    var surfaceView: SurfaceView? = null
    var callback: SurfaceHolder.Callback? = null
    var height = 240
    var width = 320
    var screenWidth = 0
    var screenHeight = 0
    var mediaCodec: MediaCodec? = null


    fun create(mSurfaceView: SurfaceView, mContext: Activity) {
        this.surfaceView = mSurfaceView
        this.context = mContext
        surfaceView!!.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surfaceView!!.keepScreenOn = true
        callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                log("this is surfaceCreated ")
                camera = open()
                getVideoSize()
                var  params = surfaceView!!.layoutParams
                params.width = width
                params.height = height
                surfaceView!!.layoutParams = params
                if (mediaRecorder == null)
                    mediaRecorder = MediaRecorder()
                else
                    mediaRecorder?.reset()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
                doChange(holder!!)
                log("this is surfaceChanged ")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                log("this is surfaceDestroyed ")
                camera?.release()
                camera = null
            }



        }
        surfaceView!!.holder.addCallback(callback)

    }

    fun doChange(holder: SurfaceHolder) {
        camera!!.setPreviewDisplay(holder)
        var x = getDisplayRotation(context!!)
        camera!!.startPreview()
    }

    fun changeCamera() {
        var cameraCount = 0
        var cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()
        for (x in 0..cameraCount) {
            log("这里是camera id $x")
            Camera.getCameraInfo(x,cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                if(camera != null){
                    camera!!.stopPreview()
                    camera!!.release()
                    camera = null
                }
                camera = Camera.open(x)
                camera!!.setPreviewDisplay(surfaceView!!.holder)
                var or = getDisplayRotation(context!!)
                camera!!.setDisplayOrientation(or)
                when (x) {
                    0 -> {
                        camera?.setDisplayOrientation(90)
                    }
                    90 -> {
                        camera?.setDisplayOrientation(0)
                    }
                    180 -> {
                        camera?.setDisplayOrientation(270)

                    }
                    270 -> {
                        camera?.setDisplayOrientation(180)
                    }
                }
                camera!!.startPreview()
                isCheck = true
                break
            }

        }


    }

    fun stopRecord() {

        camera?.lock()
        camera?.stopPreview()
        camera?.release()
        camera = null

        mediaRecorder?.setOnErrorListener(null)
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        surfaceView = null


    }

    fun destroy() {
        mediaRecorder?.setOnErrorListener(null)
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        camera?.release()
        camera = null
    }

    fun startPrepare(path: String, name: String) {

        camera?.unlock()

        if (mediaRecorder == null){
            mediaRecorder = MediaRecorder()
        }

        mediaRecorder?.setCamera(camera!!)
        //设置音视频数据源
       // mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        // 设置outPutFormat
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        // set VideoEncoder AudioEncoder
         mediaRecorder?.setVideoSource(MediaRecorder.VideoEncoder.H264)
      //  mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        mediaRecorder?.setVideoEncodingBitRate(500 * 1024)
        mediaRecorder?.setVideoSize(width, height)

        var file = File(path)
        if (!file.exists())
            file.mkdirs()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var  s = path + File.separator +".mp4"
            mediaRecorder?.setOutputFile(s)
        }

        mediaRecorder?.setPreviewDisplay(surfaceView?.holder?.surface)
        mediaRecorder?.setOrientationHint(270)
        mediaRecorder?.prepare()

    }

    fun startRecord() {
        mediaRecorder?.start()
    }

    fun getDisplayRotation(activity: Activity): Int {
        var rotation = activity.windowManager.defaultDisplay.rotation
        when (rotation) {
            Surface.ROTATION_0 -> return 0
            Surface.ROTATION_90 -> return 90
            Surface.ROTATION_180 -> return 180
            Surface.ROTATION_270 -> return 270
        }
        return 0
    }


    fun getVideoSize() {
        Log.d(TAG, "getVideoSize: ")
        val parameters = camera!!.parameters
        val videoSize =
            parameters.supportedVideoSizes
        if (videoSize != null) {
            for (i in videoSize.indices) {
                val width1 = videoSize[i].width
                val height1 = videoSize[i].height
                if (width1 == screenHeight) {
                    if (height1 == screenWidth) {
                        width = height1
                        height = width1
                        break
                    }
                }
                Log.d(
                    TAG,
                    "getVideoSize:----w:-- " + videoSize[i].width + "---h:--" + videoSize[i].height
                )
            }
            if (width != 320 && height != 240) {
                for (i in videoSize.indices) {
                    val width1 = videoSize[i].width
                    val height1 = videoSize[i].height
                    if (width1 in 300..500) {
                        if (height1 in 200..500) {
                            width = width1
                            height = height1
                            break
                        }
                    }
                    Log.d(
                        TAG,
                        "getVideoSize:----w:-- " + videoSize[i].width + "---h:--" + videoSize[i].height
                    )
                }
            }
        }
    }


    fun setVideoFrameRate() {
        val cProfile =
            CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_LOW)
        mediaRecorder!!.setProfile(cProfile)
        //设置录制的视频帧率,注意文档的说明:
        mediaRecorder!!.setVideoFrameRate(20)
    }


    fun setScreenSize(w: Int, h: Int) {
        this.screenHeight = h
        this.screenWidth = w
    }


    fun setCameraDisplayOrientation(
        activity: Activity?,
        cameraId: Int, camera: Camera
    ) {
        // See android.hardware.Camera.setCameraDisplayOrientation for
        // documentation.
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val degrees: Int = getDisplayRotation(context!!)
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }
}