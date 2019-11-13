package com.demo.ntc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.microblink.entities.recognizers.Recognizer;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdRecognizer;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.metadata.MetadataCallbacks;
import com.microblink.metadata.detection.points.DisplayablePointsDetection;
import com.microblink.metadata.detection.points.PointsDetectionCallback;
import com.microblink.metadata.detection.quad.DisplayableQuadDetection;
import com.microblink.metadata.detection.quad.QuadDetectionCallback;
import com.microblink.recognition.RecognitionSuccessType;
import com.microblink.util.CameraPermissionManager;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.exception.NonLandscapeOrientationNotSupportedException;
import com.microblink.view.recognition.RecognizerRunnerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.points.PointSetView;
import com.microblink.view.viewfinder.quadview.QuadViewManager;
import com.microblink.view.viewfinder.quadview.QuadViewManagerFactory;
import com.microblink.view.viewfinder.quadview.QuadViewPreset;
import com.microblink.image.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements BlinkIdModal.ModalListener {

    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 111;
    private static final int REQUEST_CAMERA = 222;
    private BlinkIdRecognizer recognizer;
    private RecognizerBundle recognizerBundle;
    private RecognizerRunnerView recognizerRunnerView;
    private CameraPermissionManager cameraPermissionManager;
    private Image faceImage;
    private String saveImage;

    QuadViewManager quadViewManager;
    PointSetView pointSetView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        }catch (NonLandscapeOrientationNotSupportedException e){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            recreate();
            return;
        }

        recognizerRunnerView = findViewById(R.id.recognizer_runner_view_container);
        recognizer = new BlinkIdRecognizer();
        recognizer.setReturnFaceImage(true);

        recognizerBundle = new RecognizerBundle(recognizer);

        recognizerRunnerView.setRecognizerBundle(recognizerBundle);
        recognizerRunnerView.setScanResultListener(scanResultListener);
        recognizerRunnerView.setCameraEventsListener(cameraEventListener);

        recognizerRunnerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(@NonNull Orientation orientation) {
                return false;
            }
        });

        MetadataCallbacks metadataCallbacks = new MetadataCallbacks();
        metadataCallbacks.setQuadDetectionCallback(quadDetectionCallback);
        metadataCallbacks.setPointsDetectionCallback(pointsDetectionCallback);
        recognizerRunnerView.setMetadataCallbacks(metadataCallbacks);
        cameraPermissionManager = new CameraPermissionManager(this);

        setupQuadViewManager();
        setupPointView();

        recognizerRunnerView.create();

        quadViewManager = QuadViewManagerFactory.createQuadViewFromPreset(recognizerRunnerView, QuadViewPreset.DEFAULT_FROM_DOCUMENT_SCAN_ACTIVITY);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_EXTERNAL_STORAGE);
                }
//                if(ContextCompat.checkSelfPermission(MainActivity.this,
//                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
//                }
            }
        }, 1000);

    }

    private final ScanResultListener scanResultListener = new ScanResultListener() {
        @Override
        public void onScanningDone(@NonNull RecognitionSuccessType recognitionSuccessType) {
            recognizerRunnerView.pauseScanning();
            BlinkIdRecognizer.Result result = recognizer.getResult();
            if(result.getResultState() == Recognizer.Result.State.Valid){
                faceImage = result.getFaceImage();
                Bundle bundle = new Bundle();
                bundle.putParcelable("data", result);
                BlinkIdModal modal = new BlinkIdModal();
                modal.setArguments(bundle);
                modal.show(getSupportFragmentManager(), "blinkid modal");
            }
        }
    };


    private void setupQuadViewManager(){
        quadViewManager = QuadViewManagerFactory.createQuadViewFromPreset(recognizerRunnerView, QuadViewPreset.DEFAULT_FROM_DOCUMENT_SCAN_ACTIVITY);
    }

    private void setupPointView(){
        pointSetView = new PointSetView(MainActivity.this, null, recognizerRunnerView.getHostScreenOrientation());
        recognizerRunnerView.addChildView(pointSetView, false);
    }

    private final QuadDetectionCallback quadDetectionCallback = new QuadDetectionCallback() {
        @Override
        public void onQuadDetection(@NonNull DisplayableQuadDetection displayableQuadDetection) {
            quadViewManager.animateQuadToDetectionPosition(displayableQuadDetection);
        }
    };

    private PointsDetectionCallback pointsDetectionCallback = new PointsDetectionCallback() {
        @Override
        public void onPointsDetection(@NonNull DisplayablePointsDetection displayablePointsDetection) {
            pointSetView.setDisplayablePointsDetection(displayablePointsDetection);
        }
    };

    private final CameraEventsListener cameraEventListener = new CameraEventsListener() {
        @Override
        public void onCameraPermissionDenied() {
            cameraPermissionManager.askForCameraPermission();
        }

        @Override
        public void onCameraPreviewStarted() {

        }

        @Override
        public void onCameraPreviewStopped() {

        }

        @Override
        public void onError(@NonNull Throwable throwable) {
            com.microblink.util.Log.e(this, throwable, "Error");
            AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
            ab.setMessage("there has been an error")
                    .setTitle("Error")
                    .setCancelable(false)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(dialog != null){
                                dialog.dismiss();
                            }
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }).create().show();
        }

        @Override
        public void onAutofocusFailed() {
            Toast.makeText(MainActivity.this, "Auto focus Failed", Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onAutofocusStarted(@Nullable Rect[] rects) {

        }

        @Override
        public void onAutofocusStopped(@Nullable Rect[] rects) {

        }
    };

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        recognizerBundle.saveState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(recognizerRunnerView != null){
            recognizerRunnerView.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        recognizerRunnerView.resume();
        recognizerBundle.clearSavedState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recognizerRunnerView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recognizerRunnerView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recognizerRunnerView.destroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recognizerRunnerView.changeConfiguration(newConfig);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED, null);
        finish();
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.closeBtn){
            recognizerRunnerView.resumeScanning(true);
        }
        else if(view.getId() == R.id.nextBtn){
            storeImage("face", faceImage);
            Intent intent = new Intent(this, LiveDetect.class);
            intent.putExtra("MainActivity.FILEPATH", saveImage);
            startActivity(intent);
            finish();
        }
    }

    private String createFaceBank(){

        String imagePathDir;

        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            imagePathDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/microFaces";
        }
        else {
            imagePathDir = Environment.getDataDirectory().getAbsolutePath() + "/microFaces";
        }
        Log.i(LOG_TAG, imagePathDir);
        File f = new File(imagePathDir);
        if(!f.exists()){
            f.mkdirs();
        }
        return imagePathDir;
    }

    private void storeImage(String imageName, Image image){
        String output = createFaceBank();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String dateString = dateFormat.format(new Date());
        String filename = null;
        switch (image.getImageFormat()){
            case ALPHA_8:{
                filename = output + "/alpha_8-" + imageName + "-" + dateString + ".jpg";
                break;
            }
            case BGRA_8888:{
                filename = output + "/bgra-" + imageName + "-" + dateString + ".jpg";
                break;
            }
            case YUV_NV21:{
                filename = output + "/yuv-" + imageName + "-" + dateString + ".jpg";
                break;
            }
        }
        Bitmap bitmap = image.convertToBitmap();
        if(bitmap == null){
            Log.e(LOG_TAG, "bitmap is null");
            return;
        }
        FileOutputStream fos = null;
        Log.e(LOG_TAG, filename);
        try {
            fos = new FileOutputStream(filename);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if(!success){
                Log.e(LOG_TAG, "Failed to compress bitmap");
                if(fos != null){
                    try {
                        fos.close();
                    }catch (IOException e){
                    }finally {
                        fos = null;
                    }
                    new File(filename).delete();
                }
            }
            else{
                saveImage = filename;
            }
        }catch (FileNotFoundException e){
            Log.e(LOG_TAG, "Failed to save image");
        }finally {
            if(fos != null){
                try {
                    fos.close();
                }catch (IOException ignored){
                }
            }
        }
    }
}
