package com.example.cameratest

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.sembozdemir.permissionskt.askPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {


    @RequiresApi(Build.VERSION_CODES.KITKAT)
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

        var cameraUtil = CameraUtil()
        cameraUtil.create(surface, this@MainActivity)

        var path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath

        btnBegin.setOnClickListener {
            // cameraUtil.c(surface.holder(), this@MainActivity)

            /**
             *  surface.setVisibility(View.VISIBLE);
            mCameraUtils.create(surface, MainActivity.this);
            mCameraUtils.changeCamera();
            mCameraUtils.startPrepare(VIDEO_KEEP_PATH+ File.separator+"2.mp4", "");

            new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            mCameraUtils.startRecord();
            }
            }, 2000);
             */
            surface.visibility = View.VISIBLE
            cameraUtil.create(surface, this@MainActivity)
            cameraUtil.changeCamera()
            var a = path + File.separator + "2.mp4"
            cameraUtil.startPrepare(a, "")
            Handler().postDelayed({
                cameraUtil.startRecord()
            }, 2000)

        }

        btnEnd.setOnClickListener {
            cameraUtil.stopRecord()

        }

        btnBegin.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) btnBegin.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            else
                btnBegin.setBackgroundColor(0xFFFFFF)
        }

        btnEnd.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) btnEnd.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            else
                btnEnd.setBackgroundColor(0xFFFFFF)

        }

    }
}
