package com.example.uscan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class Profile extends AppCompatActivity {
    TextView nama, email, id_karyawan, divisi, jabatan, verifymsg;
    Button resetpassword, ubahfoto;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    Button resendcode;
    String ID_Karyawan;
    FirebaseUser user;
    ImageView profileimage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        nama = findViewById(R.id.datanamalengkap);
        email = findViewById(R.id.dataemail);
        divisi = findViewById(R.id.datadivisi);
        jabatan = findViewById(R.id.datajabatan);
        id_karyawan = findViewById(R.id.dataidkaryawan);
        resetpassword = findViewById(R.id.ubahpassword);
        profileimage = findViewById(R.id.fotoprofil);
        ubahfoto = findViewById(R.id.ubahfoto);


        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        StorageReference profileRef = storageReference.child("users/"+ fAuth.getCurrentUser().getUid() +"profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(profileimage);
            }
        });

        resendcode = findViewById(R.id.resendcode);
        verifymsg = findViewById(R.id.verifymsg);

        ID_Karyawan = fAuth.getCurrentUser().getUid();
        user = fAuth.getCurrentUser();

        if (!user.isEmailVerified()) {
            verifymsg.setVisibility(View.VISIBLE);
            resendcode.setVisibility(View.VISIBLE);

            resendcode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Task<Void> tag = user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(v.getContext(), "Verifikasi Email sudah dikirim.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("tag", "on Failure: Email belum dikirim" + e.getMessage());
                        }
                    });
                }
            });
        }

        DocumentReference documentReference = fStore.collection("ID Karyawan").document(ID_Karyawan);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                if (documentSnapshot.exists()) {
                    nama.setText(documentSnapshot.getString("fName"));
                    email.setText(documentSnapshot.getString("Email"));
                    id_karyawan.setText(documentSnapshot.getString("IDkwn"));
                    divisi.setText(documentSnapshot.getString("divis"));
                    jabatan.setText(documentSnapshot.getString("tgkjb"));

                } else {
                    Log.d("tag", "onEvent: Dokumen belum tersedia");
                }

            }
        });

        ubahfoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGalleryIntent, 1000);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = data.getData();

                //profileimage.setImageURI(imageUri);

                upluadimagetofirebase(imageUri);
            }
        }

    }

    private void upluadimagetofirebase(Uri imageUri) {
        StorageReference fileRef = storageReference.child("users/"+ fAuth.getCurrentUser().getUid() +"profile.jpg");
         fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).into(profileimage);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Profile.this,"Gagal upload", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void logout (View view) {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getApplicationContext(), login.class));
        finish();
    }

    public void gantipassword(View view) {
        EditText resetMail = new EditText (view.getContext());
        AlertDialog.Builder passwordresetdialog = new AlertDialog.Builder(view.getContext());
        passwordresetdialog.setTitle("Apakah Anda Mau Mengatur Ulang Password?");
        passwordresetdialog.setMessage("Masukkan Email Anda untuk mendapatkan Laman Pengturan Ulang.");
        passwordresetdialog.setView(resetMail);

        passwordresetdialog.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mail = resetMail.getText().toString();
                fAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(Profile.this, "Laman Reset Telah Dikirim ke Email Anda", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Profile.this, "Gagal! Laman Reset Tidak Terkirim", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        passwordresetdialog.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        passwordresetdialog. create().show();
    }
}