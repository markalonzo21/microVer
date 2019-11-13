package com.demo.ntc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.exifinterface.media.ExifInterface;

import com.appliedrec.rxverid.RxVerID;
import com.appliedrec.verid.core.AuthenticationSessionSettings;
import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.FaceDetectionRecognitionFactory;
import com.appliedrec.verid.core.FaceDetectionRecognitionSettings;
import com.appliedrec.verid.ui.VerIDSessionIntent;

import org.javatuples.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LiveDetect extends AppCompatActivity {

    private Uri faceImageUri;
    private ImageView imageView;
    private RxVerID rxVerID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_detect);

        faceImageUri = Uri.fromFile(new File(getIntent().getStringExtra("MainActivity.FILEPATH")));

        FaceDetectionRecognitionSettings settings = new FaceDetectionRecognitionSettings(null);
        FaceDetectionRecognitionFactory faceDetectionRecognitionFactory = new FaceDetectionRecognitionFactory(
                this, null, settings);

        rxVerID = new RxVerID.Builder(this)
                .setFaceDetectionFactory(faceDetectionRecognitionFactory)
                .setFaceRecognitionFactory(faceDetectionRecognitionFactory)
                .build();

        imageView = findViewById(R.id.imageView);

        rxVerID.detectFacesInImage(faceImageUri, 1)
                .firstOrError().flatMap(face -> rxVerID.cropImageToFace(faceImageUri, face))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(imageView::setImageBitmap,
                        error -> Toast.makeText(LiveDetect.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
    }
}
