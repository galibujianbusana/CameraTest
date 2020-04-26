package com.example.cameratest

import android.app.Activity
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.open
import android.media.CamcorderProfile
import android.media.MediaCodec
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File

/**
 * @ClassName CameraUtil
 * @Description TODO
 * @Date 2020/4/24 13:51
 */
class CameraUtil {


    companion object {
        private const val TAG = "CameraUtil"
        fun log(string: String) {
            Log.d(TAG, string)
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
                var params = surfaceView!!.layoutParams
                params.width = width
                params.height = height
                surfaceView!!.layoutParams = params
                if (mediaRecorder == null)
                    mediaRecorder = MediaRecorder()
                else
                    mediaRecorder!!.reset()
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
                if (camera != null) {
                    camera?.release()
                    camera = null
                }
            }


        }
        surfaceView!!.holder.addCallback(callback)

    }

    fun doChange(holder: SurfaceHolder) {
        camera!!.setPreviewDisplay(holder)
        var x = getDisplayRotation(context!!)
        log("这里设置旋转方向2: $x - - 113 ." )
        if (x == 90) {
            camera!!.setDisplayOrientation(0)
        } else if (x == 0) {
            camera!!.setDisplayOrientation(90)
        }
        camera!!.startPreview()
    }

    fun changeCamera() {
        var cameraCount = 0
        var cameraInfo = Camera.CameraInfo()
        cameraCount = Camera.getNumberOfCameras()
        for (x in 0..cameraCount) {
            log("这里是camera id $x")
            Camera.getCameraInfo(x, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {

                camera?.stopPreview()
                camera?.release()
                camera = null

                camera = Camera.open(x)
                camera!!.setPreviewDisplay(surfaceView!!.holder)
                var or = getDisplayRotation(context!!)
                camera!!.setDisplayOrientation(or)
                log("这里设置旋转方向1: $or - - 113 ." )
                if (x == 90) {
                    camera!!.setDisplayOrientation(0)
                } else if (x == 0) {
                    camera!!.setDisplayOrientation(90)
                }
                camera!!.startPreview()
                isCheck = true
                break
            }

        }


    }

    fun stopRecord() {

        camera!!.lock()
        camera!!.stopPreview()
        camera!!.release()
        camera = null

        mediaRecorder!!.setOnErrorListener(null)
        mediaRecorder!!.stop()
        mediaRecorder!!.release()
        mediaRecorder = null
        surfaceView = null


    }

    fun destroy() {
        mediaRecorder!!.setOnErrorListener(null)
        mediaRecorder!!.stop()
        mediaRecorder!!.release()
        mediaRecorder = null
        camera?.release()
        camera = null
    }

    fun startPrepare(path: String, name: String) {

        camera!!.unlock()

        if (mediaRecorder == null) {
            mediaRecorder = MediaRecorder()
        }

        mediaRecorder?.setCamera(camera!!)
        //设置音视频数据源
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        // 设置outPutFormat
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        // set VideoEncoder AudioEncoder
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        mediaRecorder?.setVideoEncodingBitRate(500 * 1024)
        mediaRecorder?.setVideoSize(width, height)

        var file = File(path)
        if (!file.exists())
            file.mkdirs()


        mediaRecorder?.setOutputFile(path)
        var fileKeep = File(path)
        if (fileKeep.exists()) fileKeep.delete()


        mediaRecorder?.setPreviewDisplay(surfaceView?.holder?.surface)
        //控制录制视频的方向
        mediaRecorder?.setOrientationHint(90)
        mediaRecorder?.prepare()

    }

    fun startRecord() {
        mediaRecorder?.start()
    }

    private fun getDisplayRotation(activity: Activity): Int {
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
        log("getVideoSize: ")
        val parameters = camera!!.parameters
        val videoSize = parameters.supportedVideoSizes ?: return
        for (i in videoSize.indices) {
            val width1 = videoSize[i].width
            val height1 = videoSize[i].height
            if (width1 == 1920 && height1 == 1080) {
                width = width1
                height = height1
                return
            }
            log("getVideoSize:----w:-- " + videoSize[i].width + "---h:--" + videoSize[i].height)
        }
        if (width != 1920 && height != 1080) {
            for (i in videoSize.indices) {
                val width1 = videoSize[i].width
                val height1 = videoSize[i].height
                if (width1 in 1000..1500) {
                    if (height1 in 600..920) {
                        width = width1
                        height = height1
                        break
                    }
                }
                log("getVideoSize:----w:-- " + videoSize[i].width + "---h:--" + videoSize[i].height)
            }
        }
        log("最后的寛高为: getVideoSize  - - > $width - - $height")

    }


    fun setVideoFrameRate() {
        val cProfile =
            CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_LOW)
        mediaRecorder!!.setProfile(cProfile)
        //设置录制的视频帧率,注意文档的说明:
        mediaRecorder!!.setVideoFrameRate(20)
    }





    fun setCameraDisplayOrientation(activity: Activity?, cameraId: Int, camera: Camera) {
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