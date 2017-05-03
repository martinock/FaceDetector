package com.martinock.facedetector;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.contract.TrainingStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 1;
    private final String personGroupId = "ifitb";

    private ImageView imagePreview;
    private Bitmap imageBitmap;
    private LinearLayout llActionButtons;

    private FaceServiceClient faceServiceClient;
    Face[] facesDetected;

    private class DetectTask extends AsyncTask<InputStream, String, Face[]> {

        private ProgressDialog mProgressDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected Face[] doInBackground(InputStream... params) {
            try {
                publishProgress("Detecting Faces...");
                Face[] results = faceServiceClient.detect(params[0], true, false, null);
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
        protected void onPostExecute(final Face[] faces) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Detection Finished. Detected " + faces.length + " face(s)." ,Toast.LENGTH_LONG).show();
                }
            });
            mProgressDialog.dismiss();
            facesDetected = faces;
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            mProgressDialog.dismiss();
            mProgressDialog.show();
            mProgressDialog.setMessage(values[0]);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getApplicationContext(), values[0] ,Toast.LENGTH_LONG).show();
//                }
//            });
        }
    }

    private class IdentificationTask extends  AsyncTask<UUID, String, IdentifyResult[]> {
        private String personGroupId;
        private ProgressDialog mProgressDialog = new ProgressDialog(MainActivity.this);

        public IdentificationTask(String personGroupId) {
            this.personGroupId = personGroupId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            try {
                publishProgress("Getting person group status...");
                TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(
                        this.personGroupId);
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status);
                    return null;
                }
                publishProgress("Identifying...");

                return faceServiceClient.identity(personGroupId, params, 1);
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
        protected void onPostExecute(IdentifyResult[] identifyResults) {
            mProgressDialog.dismiss();

            int i = 0;
            for(IdentifyResult identifyResult:identifyResults) {
                new PersonDetectionTask(personGroupId, i).execute(identifyResult
                        .candidates.get(0).personId);
                i++;
            }
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            mProgressDialog.dismiss();
            mProgressDialog.show();
            mProgressDialog.setMessage(values[0]);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getApplicationContext(), values[0] ,Toast.LENGTH_LONG).show();
//                }
//            });
        }
    }

    private class PersonDetectionTask extends AsyncTask<UUID, String, Person> {
        private ProgressDialog mProgressDialog = new ProgressDialog(MainActivity.this);
        private String personGroupId;
        private int idx;

        public PersonDetectionTask(String personGroupId, int idx) {
            this.personGroupId = personGroupId;
            this.idx = idx;
        }

        @Override
        protected Person doInBackground(UUID... params) {
            try {
                publishProgress("Getting person group status...");
                return faceServiceClient.getPerson(personGroupId, params[0]);
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
        protected void onPostExecute(Person person) {
            mProgressDialog.dismiss();

            imageBitmap = drawFaceRectangleOnBitmap(imageBitmap,
                    facesDetected[idx], person.name);
            imagePreview.setImageBitmap(imageBitmap);
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            mProgressDialog.dismiss();
            mProgressDialog.show();
            mProgressDialog.setMessage(values[0]);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getApplicationContext(), values[0] ,Toast.LENGTH_LONG).show();
//                }
//            });
        }
    }

    private Bitmap drawFaceRectangleOnBitmap(Bitmap imageBitmap,
                                             Face face, String name) {
        Bitmap bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(12);

        if (facesDetected != null) {
            FaceRectangle faceRectangle = face.faceRectangle;
            canvas.drawRect(
                    faceRectangle.left,
                    faceRectangle.top,
                    faceRectangle.left + faceRectangle.width,
                    faceRectangle.top + faceRectangle.height,
                    paint);
            drawTextOnCanvas(
                    canvas,
                    100,
                    ((faceRectangle.left + faceRectangle.width) / 2) + 100,
                    (faceRectangle.top + faceRectangle.height) + 50,
                    Color.WHITE,
                    name);
        }
        return bitmap;
    }

    private void drawTextOnCanvas(Canvas canvas, int textSize, int x, int y, int color, String name) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(textSize);

        float textWidth = paint.measureText(name);
        canvas.drawText(name, x-(textWidth/2), y - (textSize/2), paint);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        faceServiceClient = new FaceServiceRestClient("5388739dcaf64c4e8fd2a5a37a132788");

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

    private void detectFace() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        new DetectTask().execute(inputStream);
    }

    private void identifyFace() {
        final UUID[] faceIds = new UUID[facesDetected.length];
        for (int i = 0; i < facesDetected.length; ++i) {
            faceIds[i] = facesDetected[i].faceId;
        }

        new IdentificationTask(personGroupId).execute(faceIds);
    }
}