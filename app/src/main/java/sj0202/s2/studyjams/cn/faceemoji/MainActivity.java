package sj0202.s2.studyjams.cn.faceemoji;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
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

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.firebase.analytics.FirebaseAnalytics;

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
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private Mat rgbaFace;
    private static final String TAG = "FaceCV::Main";
    private int REQUEST_INVITE = 0;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String filename;

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

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCameraIndex(CAMERA_ID);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        ImageButton rotateButton = (ImageButton) findViewById(R.id.rotate_button);
        ImageButton captureButton = (ImageButton) findViewById(R.id.capture_button);
        ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);

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

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bmp = null;
                bmp = Bitmap.createBitmap(rgbaFace.cols(), rgbaFace.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgbaFace, bmp);
                FileOutputStream out = null;
                filename ="IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".png";
                File sd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        + "/FaceEmoji");
                boolean success = true;
                if (!sd.exists()) {
                    success = sd.mkdir();
                }
                if (success) {
                    File dest = new File(sd, filename);
                    try {
                        out = new FileOutputStream(dest);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.close();
                        Toast.makeText(getApplicationContext(),"保存成功",Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, e.getMessage());
                    }
                }else{
                    Log.e(TAG,"FILE NOT OK");
                }
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                if(filename==null){
                    Toast.makeText(getApplicationContext(),"先拍摄图片",Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        + "/FaceEmoji"+filename);
                intent.setType("image/png");
                Uri uri = Uri.fromFile(file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.putExtra(Intent.EXTRA_TEXT, "来自FaceEmoji");
                startActivity(Intent.createChooser(intent, "FaceEmoji"));
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
        rgbaFace = inputFrame.rgba();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(),"分享成功",Toast.LENGTH_SHORT);
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                for (String id : ids) {
                    Log.d(TAG, "onActivityResult: sent invitation " + id);
                }
            } else {
                Toast.makeText(getApplicationContext(),"分享失败",Toast.LENGTH_SHORT);
            }
        }
    }
}

