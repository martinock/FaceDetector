package com.martinock.facedetector;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 1;

    private ImageView imagePreview;
    private Bitmap imageBitmap;
    private Bitmap resultBitmap;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnBrowse = (Button) findViewById(R.id.btn_browse);
        Button btnProcess = (Button) findViewById(R.id.btn_process);
        imagePreview = (ImageView) findViewById(R.id.image_preview);

        final Paint paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        btnBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, SELECT_PICTURE);
            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProcessImage(paint);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri imageUri = data.getData();
                if (imageUri == null) {
                    Toast.makeText(getApplicationContext(),
                            "Image Get Error", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        imageBitmap = BitmapFactory.decodeStream(imageStream);
                        imagePreview.setImageBitmap(imageBitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                "Oops! Something went wrong", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private void ProcessImage(final Paint p) {
        mProgressDialog = ProgressDialog.show(this, getString(R.string.loading),
                getString(R.string.please_wait), true, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (imageBitmap != null) {
                    resultBitmap = Bitmap.createBitmap(imageBitmap.getWidth(),
                            imageBitmap.getHeight(), Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(resultBitmap);
                    canvas.drawBitmap(imageBitmap, 0, 0, null);
                    com.google.android.gms.vision.face.FaceDetector faceDetector = new
                            com.google.android.gms.vision.face.FaceDetector.Builder(
                            getApplicationContext())
                            .setTrackingEnabled(false)
                            .setLandmarkType(com.google.android.gms.vision.face.FaceDetector.ALL_LANDMARKS)
                            .setMode(com.google.android.gms.vision.face.FaceDetector.FAST_MODE)
                            .build();
                    if (!faceDetector.isOperational()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                                Toast.makeText(getApplicationContext(),
                                        "Face Detector could not be set up on your device",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }

                    Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
                    SparseArray<Face> sparseArray = faceDetector.detect(frame);

                    for (int i = 0; i < sparseArray.size(); ++i) {
                        Face face = sparseArray.valueAt(i);
                        float x1 = face.getPosition().x;
                        float y1 = face.getPosition().y;
                        float x2 = x1 + face.getWidth();
                        float y2 = y1 + face.getHeight();
                        RectF rectF = new RectF(x1, y1, x2, y2);
                        canvas.drawRoundRect(rectF, 2, 2, p);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            imagePreview.setImageDrawable(new BitmapDrawable(getResources(),
                                    resultBitmap));
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            Toast.makeText(getApplicationContext(),
                                    "You haven't select an image", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}