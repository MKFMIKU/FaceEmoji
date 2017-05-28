package sj0202.s2.studyjams.cn.faceemoji;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity{
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private int CAMERA_ID = CameraBridgeViewBase.CAMERA_ID_BACK;
    private Mat mRgba;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private static final String TAG = "FaceCV::Main";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    System.loadLibrary("native-lib");
                    cameraBridgeViewBase.enableView();
                } break;
                default:{
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    private void init() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }else{
            Toast.makeText(this,"Permission OK!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        init();

        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCameraIndex(CAMERA_ID);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                mRgba = new Mat(width, height, CvType.CV_8UC4);
            }

            @Override
            public void onCameraViewStopped() {
                mRgba.release();
            }

            @Override
            public Mat onCameraFrame(Mat inputFrame) {
                return inputFrame;
            }

        });

        ImageButton rotateButton = (ImageButton) findViewById(R.id.rotate_button);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraBridgeViewBase.disableView();
                if(CAMERA_ID==CameraBridgeViewBase.CAMERA_ID_BACK)
                    CAMERA_ID = CameraBridgeViewBase.CAMERA_ID_FRONT;
                else CAMERA_ID=CameraBridgeViewBase.CAMERA_ID_BACK;
                cameraBridgeViewBase.setCameraIndex(CAMERA_ID);
                cameraBridgeViewBase.enableView();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults){
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if(grantResults.length>0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                }else{
                    Toast.makeText(this,"请确认摄像头权限",Toast.LENGTH_LONG).show();
                }
                init();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV loaded");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
