package com.example.uscan.faceemr;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uscan.MainActivity;
import com.example.uscan.Profile;
import com.example.uscan.R;
import com.example.uscan.predictivemodels.*;
import com.example.uscan.register;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FaceRec extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    public static final String TAG = "TAG";
    private SquareImageView faceImageView;
    private TextView emotionShowView, currentdate;
    private Button attendance;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int PIXEL_WIDTH = 48;
    TensorFlowClassifier classifier;
    Button detect;
    String UserID;
    private FirebaseStorage storage;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facerec);

        attendance          = findViewById(R.id.storeattendance);
        fAuth               = FirebaseAuth.getInstance();
        fStore              = FirebaseFirestore.getInstance();
        storage             = FirebaseStorage.getInstance();
        storageReference    = storage.getReference();

        //ini kodingan button buat store "timestamp" sama "score expressionnya"

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                currentdate         = findViewById(R.id.tanggal);
                                long date = System.currentTimeMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, hh:mm:ss");
                                String dateString = sdf.format(date);
                                currentdate.setText(dateString);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();

        this.faceImageView = (SquareImageView) this.findViewById(R.id.facialImageView);
        Button photoButton = (Button) this.findViewById(R.id.phototaker);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        detect = (Button) findViewById(R.id.detect);
        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectEmotion();
            }
        });
        Button reset = (Button) findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearStatus();
            }
        });
        detect.setEnabled(false);
        this.emotionShowView = (TextView) findViewById(R.id.emotionTxtView);
        loadModel();

        attendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String attendancescore = emotionShowView.getText().toString().trim();
                if (TextUtils.equals(attendancescore,"Status: ?")) {
                    emotionShowView.setError("Silahkan absensi terlebih dahulu");
                    return;
                }
                UserID =fAuth.getCurrentUser().getUid();
                DocumentReference documentReference =fStore.collection("Absensi Karyawan").document(UserID);
                Map<String, Object> absensi = new HashMap<>();
                absensi.put("Waktu kehadiran", FieldValue.serverTimestamp());
                absensi.put("Nilai Kehadiran", emotionShowView.getText());
                documentReference.set(absensi).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Anda sudah melakukan absensi"+ UserID);
                    }
                });
                documentReference.set(absensi).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onSuccess: Anda belum melakukan absensi" + e.toString());
                    }
                });
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });
    }

    //Ini kodingan buat face recognitionnya

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    private void loadModel() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier=TensorFlowClassifier.create(getAssets(), "CNN",
                            "opt_em_convnet_5000.pb", "labels.txt", PIXEL_WIDTH,
                            "input", "output_50", true, 7);

                } catch (final Exception e) {
                    //if they aren't found, throw an error!
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    /**
     * The main function for emotion detection
     */
    private void detectEmotion(){

        Bitmap image=((BitmapDrawable)faceImageView.getDrawable()).getBitmap();
        Bitmap grayImage = toGrayscale(image);
        Bitmap resizedImage=getResizedBitmap(grayImage,48,48);
        int pixelarray[];


        //Initialize the intArray with the same size as the number of pixels on the image
        pixelarray = new int[resizedImage.getWidth()*resizedImage.getHeight()];

        //copy pixel data from the Bitmap into the 'intArray' array
        resizedImage.getPixels(pixelarray, 0, resizedImage.getWidth(), 0, 0, resizedImage.getWidth(), resizedImage.getHeight());


        float normalized_pixels [] = new float[pixelarray.length];
        for (int i=0; i < pixelarray.length; i++) {
            // 0 for white and 255 for black
            int pix = pixelarray[i];
            int b = pix & 0xff;
            //  normalized_pixels[i] = (float)((0xff - b)/255.0);
            // normalized_pixels[i] = (float)(b/255.0);
            normalized_pixels[i] = (float)(b);

        }
        System.out.println(normalized_pixels);
        Log.d("pixel_values", String.valueOf(normalized_pixels));
        String text=null;
        String label;
        float confidence = 0;


        try{
            final Classification res = classifier.recognize(normalized_pixels);
            //if it can't classify, output a question mark
            if (res.getLabel() == null) {
                text = "Status: "+ ": ?\n";
            } else {
                //else output its name
                text = String.format("%s: %s, %f\n", "Status: ", res.getLabel(), res.getConf());
                label = res.getLabel();
                confidence = res.getConf();
            }}
        catch (Exception e){
            System.out.print("Exception:"+e.toString());

        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] dataAttd = baos.toByteArray();

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Uploading Image");
        pd.show();

        final String randomKey = UUID.randomUUID().toString();
        StorageReference attendanceRef = storageReference.child("FotoAbsensi/" + randomKey);
        attendanceRef.putBytes(dataAttd).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                pd.dismiss();
                Toast.makeText(getApplicationContext(),"Foto Berhasil Diupload", Toast.LENGTH_LONG).show();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(getApplicationContext(),"Gagal Upload Foto", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        pd.setMessage("Progress: " + (int) progressPercent + "%");
                    }
                });



        this.faceImageView.setImageBitmap(grayImage);
        this.emotionShowView.setText(text);

    }

    /**
     *
     */
    private void clearStatus(){
        detect.setEnabled(false);
        this.faceImageView.setImageResource(R.drawable.ic_launcher_background);
        this.emotionShowView.setText("Status: ?");

    }

    /**
     *
     * @param bmpOriginal
     * @return
     */
    // https://stackoverflow.com/questions/3373860/convert-a-bitmap-to-grayscale-in-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    //https://stackoverflow.com/questions/15759195/reduce-size-of-bitmap-to-some-specified-pixel-in-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
    public Bitmap getResizedBitmap(Bitmap image, int bitmapWidth, int bitmapHeight) {
        return Bitmap.createScaledBitmap(image, bitmapWidth, bitmapHeight, true);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            detect.setEnabled(true);
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            faceImageView.setImageBitmap(imageBitmap);
        }
    }
}
