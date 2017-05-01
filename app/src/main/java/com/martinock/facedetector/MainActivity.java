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
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.Frame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 1;
    private static final String PERSON_GROUP_IF = "ifitb";

    private ImageView imagePreview;
    private Bitmap imageBitmap;
    private Bitmap resultBitmap;
    private LinearLayout llActionButtons;
    private ProgressDialog mProgressDialog;

    private FaceServiceClient faceServiceClient;
    com.microsoft.projectoxford.face.contract.Face[] facesDetected;

    class DetectTask extends AsyncTask<InputStream, String,
            com.microsoft.projectoxford.face.contract.Face[]> {

        private ProgressDialog mProgressDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected com.microsoft.projectoxford.face.contract.Face[] doInBackground(InputStream... params) {
            try {
                publishProgress("Detecting Faces...");
                com.microsoft.projectoxford.face.contract.Face[] results = faceServiceClient.detect(params[0], true, false, null);
                if (results == null) {
                    publishProgress("No face detected. Please upload another image");
                    return null;
                } else {
                    if (results.length == 0) {
                        publishProgress("No face detected. Please upload another image");
                        return null;
                    } else {
                        publishProgress("Detection Finished. Detected " + results.length + " face(s).");
                        return results;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            mProgressDialog.setMessage(getString(R.string.please_wait));
        }

        @Override
        protected void onPostExecute(com.microsoft.projectoxford.face.contract.Face[] faces) {
            mProgressDialog.dismiss();
            facesDetected = faces;
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), values[0] ,Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        faceServiceClient = new FaceServiceRestClient("29a6d137accb425a8b2e5f8941fb0f4d");

        Button btnBrowse = (Button) findViewById(R.id.btn_browse);
        Button btnIdentify = (Button) findViewById(R.id.btn_identify);
        Button btnDetect = (Button) findViewById(R.id.btn_detect);
        llActionButtons = (LinearLayout) findViewById(R.id.ll_action_buttons);
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

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectFace();
            }
        });

        btnIdentify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                identifyFace();
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
                        llActionButtons.setVisibility(View.VISIBLE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                "Oops! Something went wrong", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    /* Detect faces on a picture */
    private void getFace(final Paint p) {
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

    private void detectFace() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        new DetectTask().execute(inputStream);
    }

    private void identifyFace() {

    }
}