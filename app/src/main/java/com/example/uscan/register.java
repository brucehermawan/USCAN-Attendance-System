package com.example.uscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {
    public static final String TAG = "TAG";
    EditText mnamalengkap, memail, midkaryawan, mnomorhp, mpassword, mtanggallahir, mjabatan, mdivisi;
    Button mRegisterBtn, mpilihtanggal;
    TextView mLoginBtn;
    DatePickerDialog.OnDateSetListener setListener;
    FirebaseAuth fAuth;
    ProgressBar progressBar;
    FirebaseFirestore fStore;
    String UserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mnamalengkap        = findViewById(R.id.namalengkap);
        memail              = findViewById(R.id.email);
        midkaryawan         = findViewById(R.id.idkaryawan);
        mnomorhp            = findViewById(R.id.nomorhp);
        mpassword           = findViewById(R.id.password);
        mRegisterBtn        = findViewById(R.id.register);
        mLoginBtn           = findViewById(R.id.logintextview);
        mjabatan            = findViewById(R.id.jabatan);
        mdivisi             = findViewById(R.id.divisi);

        mtanggallahir       = findViewById(R.id.tanggallahir);
        mpilihtanggal       = findViewById(R.id.pilihtanggal);
        Calendar calendar   = Calendar.getInstance();
        final int year      = calendar.get(Calendar.YEAR);
        final int month     = calendar.get(Calendar.MONTH);
        final int day       = calendar.get(Calendar.DAY_OF_MONTH);

        mpilihtanggal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog =new DatePickerDialog(
                        register.this, android.R.style.Theme_DeviceDefault_Dialog_MinWidth
                        , setListener, year, month, day);
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLUE));
                datePickerDialog.show();
            }
        });
        setListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month+1;
                String date = dayOfMonth+"/"+month+"/"+year;
                mtanggallahir.setText(date);
            }
        };
        mtanggallahir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(register.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month = month + 1;
                        String date = dayOfMonth + "/" + month + "/" + year;
                        mtanggallahir.setText(date);
                    }
                }, year, month, day);
                datePickerDialog.show();
            }
        });

        fAuth               = FirebaseAuth.getInstance();
        fStore              = FirebaseFirestore.getInstance();
        progressBar         = findViewById(R.id.progressbar);

        if (fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
        mRegisterBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String namalengkap = mnamalengkap.getText().toString().trim();
                String email = memail.getText().toString().trim();
                String idkaryawan = midkaryawan.getText().toString();
                String nomorhp = mnomorhp.getText().toString();
                String password = mpassword.getText().toString();
                String tanggallahir = mtanggallahir.getText().toString();
                String jabatan = mjabatan.getText().toString();
                String divisi = mdivisi.getText().toString();

                if (TextUtils.isEmpty(namalengkap)) {
                    mnamalengkap.setError("Wajib untuk mengisi nama lengkap Anda");
                    return;
                }
                if (TextUtils.isEmpty(tanggallahir)) {
                    mtanggallahir.setError("Wajib untuk mengisi tanggal lahir Anda");
                    return;
                }
                if (TextUtils.isEmpty(jabatan)) {
                    mjabatan.setError("Wajib untuk mengisi tingkat jabatan Anda");
                    return;
                }
                if (TextUtils.isEmpty(divisi)) {
                    mdivisi.setError("Wajib untuk mengisi divisi Anda");
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    memail.setError("Wajib untuk mengisi email Anda");
                    return;
                }
                if (TextUtils.isEmpty(idkaryawan)) {
                    midkaryawan.setError("Wajib untuk mengisi nomor HP Anda");
                    return;
                }
                if (TextUtils.isEmpty(nomorhp)) {
                    mnomorhp.setError("Wajib untuk mengisi nomor HP Anda");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    mpassword.setError("Wajib untuk mengisi password Anda");
                    return;
                }
                if (password.length() < 7) {
                    mpassword.setError("Password harus lebih dari 7 Karakter");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser fuser = fAuth.getCurrentUser();
                            fuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(register.this, "Verifikasi Email sudah dikirim.", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "on Failure: Email belum dikirim" + e.getMessage());
                                }
                            });

                            Toast.makeText(register.this, "Akun telah dibuat.", Toast.LENGTH_SHORT).show();
                            UserID = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("ID Karyawan").document(UserID);
                            Map<String, Object> user = new HashMap<>();
                            user.put("fName", namalengkap);
                            user.put("tglhr", tanggallahir);
                            user.put("Email", email);
                            user.put("IDkwn", idkaryawan);
                            user.put("Phone", nomorhp);
                            user.put("divis", divisi);
                            user.put("tgkjb", jabatan);
                            user.put("Passw", password);
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "onSuccess: Profil pengguna dibuat untuk" + UserID);
                                }
                            });
                            documentReference.set(user).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "onSuccess: Profil pengguna dibuat untuk" + e.toString());
                                }
                            });
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));

                        } else {
                            Toast.makeText(register.this, "Maaf, Akun Tersebut Sudah Terdaftar", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(register.this, login.class));
            }
        });
    }
}