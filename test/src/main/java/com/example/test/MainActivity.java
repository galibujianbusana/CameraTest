package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private CameraUtils mCameraUtils;
    private SurfaceView surface;
    private String VIDEO_KEEP_PATH = "";

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
      /*  askPermissions(
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
        }*/

        mCameraUtils = new CameraUtils();
        surface = findViewById(R.id.surface);

        VIDEO_KEEP_PATH =  getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();

        findViewById(R.id.btnBegin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                surface.setVisibility(View.VISIBLE);
                mCameraUtils.create(surface, MainActivity.this);
                mCameraUtils.changeCamera();
                mCameraUtils.startPrepare(VIDEO_KEEP_PATH+ File.separator+"2.mp4", "");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCameraUtils.startRecord();
                    }
                }, 2000);
            }
        });


        findViewById(R.id.btnEnd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraUtils.stopRecord();

            }
        });
    }


    private void requestPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.Group.STORAGE, Permission.Group.CAMERA, Permission.Group.MICROPHONE)
                    .onGranted(data -> runOnUiThread(() -> {
                    }))
                    .onDenied(data -> {
                        if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, data)) {
                            AndPermission.permissionSetting(MainActivity.this).execute();
                            return;
                        }
                    })
                    .start();
        }
    }
}
