package sj0202.s2.studyjams.cn.faceemoji;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private int CAMERA_ID = CameraBridgeViewBase.CAMERA_ID_BACK;
    private CascadeClassifier cascadeClassifier;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private int absoluteFaceSize;
    private Mat mEmojiRgba;
    private Mat mEmojiMask;
    private Mat mEmojiRgbaScale;
    private Mat mEmojiMaskScale;
    private static final String TAG = "FaceCV::Main";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    System.loadLibrary("native-lib");

                    try{
                        InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                        File cascadeDir = getDir("cascade",Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir,"haarcascade_frontalface_default.xml");
                        FileOutputStream outputStream = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead= inputStream.read(buffer))!=-1){
                            outputStream.write(buffer,0,bytesRead);
                        }
                        inputStream.close();
                        outputStream.close();
                        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    }catch (IOException e){
                        Log.e(TAG,"Error cascade",e);
                    }
                    cameraBridgeViewBase.enableView();
                } break;
                default:{
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},1);

        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCameraIndex(CAMERA_ID);
        cameraBridgeViewBase.setCvCameraViewListener(this);

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
    public void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults){
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if(grantResults.length>0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                }else {
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    public void disableCamera() {
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        absoluteFaceSize = (int) (height*0.2);
        mEmojiRgba = new Mat();
        mEmojiMask = new Mat();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.emoji_haha);
        Utils.bitmapToMat(bitmap,mEmojiRgba);
        Imgproc.cvtColor(mEmojiRgba,mEmojiMask,Imgproc.COLOR_RGB2GRAY);
    }

    @Override
    public void onCameraViewStopped() {
        mEmojiRgba.release();
        mEmojiMask.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat grayFace = inputFrame.gray();
        Mat rgbaFace = inputFrame.rgba();
        MatOfRect faces = new MatOfRect();

        mEmojiRgbaScale = new Mat();
        mEmojiMaskScale = new Mat();

        if(cascadeClassifier!=null){
            cascadeClassifier.detectMultiScale(grayFace,faces,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }

        Rect[] faceArray = faces.toArray();
        for(int i=0;i<faceArray.length;i++){
            if(faceArray[i].x+mEmojiRgba.width()<rgbaFace.width() &&
                    faceArray[i].y+mEmojiRgba.height()<rgbaFace.height()){
                Imgproc.resize(mEmojiRgba,mEmojiRgbaScale,faceArray[i].size());
                Imgproc.resize(mEmojiMask,mEmojiMaskScale,faceArray[i].size());
                Mat imageRoi = rgbaFace.submat(new Rect(faceArray[i].x,faceArray[i].y,
                        mEmojiRgbaScale.width(),mEmojiRgbaScale.height()));
                mEmojiRgbaScale.copyTo(imageRoi,mEmojiMaskScale);
            }
        }
        return rgbaFace;
    }
}

