package com.example.quiz_application;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    CircleImageView profileImage;
    EditText etName;
    TextView tvEmail, tvScore, tvLocation;
    Button btnChangePhoto, btnSave, btnLogout, btnGetLocation;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FusedLocationProviderClient fusedLocationClient;

    Uri selectedImageUri = null;
    Uri cameraImageUri = null;

    // Launcher galerie
    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    profileImage.setImageURI(selectedImageUri);
                }
            }
    );

    // Launcher caméra
    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    selectedImageUri = cameraImageUri;
                    profileImage.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Init vues
        profileImage    = findViewById(R.id.profileImage);
        etName          = findViewById(R.id.etName);
        tvEmail         = findViewById(R.id.tvEmail);
        tvScore         = findViewById(R.id.tvScore);
        tvLocation      = findViewById(R.id.tvLocation);
        btnChangePhoto  = findViewById(R.id.btnChangePhoto);
        btnSave         = findViewById(R.id.btnSave);
        btnLogout       = findViewById(R.id.btnLogout);
        btnGetLocation  = findViewById(R.id.btnGetLocation);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Init GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Charger profil en temps réel
        loadProfile();

        btnChangePhoto.setOnClickListener(v -> showPhotoDialog());
        btnSave.setOnClickListener(v -> saveProfile());
        btnGetLocation.setOnClickListener(v -> getLocation());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // 📥 Charger profil depuis Firestore en temps réel
    private void loadProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference docRef = db.collection("users").document(uid);

        docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) return;
            if (snapshot != null && snapshot.exists()) {
                etName.setText(snapshot.getString("name"));
                tvEmail.setText(snapshot.getString("email"));

                Long score = snapshot.getLong("score");
                tvScore.setText((score != null ? score : 0) + " / 5");

                // Charger localisation
                String location = snapshot.getString("location");
                if (location != null && !location.isEmpty()) {
                    tvLocation.setText(location);
                }

                // Charger photo Base64
                String photoBase64 = snapshot.getString("photoBase64");
                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    byte[] imageBytes = Base64.decode(photoBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            imageBytes, 0, imageBytes.length);
                    profileImage.setImageBitmap(bitmap);
                }
            }
        });
    }

    // 📍 Obtenir la position GPS
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        tvLocation.setText("📍 Chargement...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(
                                    location.getLatitude(),
                                    location.getLongitude(), 1);

                            if (addresses != null && !addresses.isEmpty()) {
                                String city    = addresses.get(0).getLocality();
                                String country = addresses.get(0).getCountryName();
                                String locationText = "📍 " + city + ", " + country;
                                tvLocation.setText(locationText);

                                // Sauvegarder dans Firestore
                                String uid = mAuth.getCurrentUser().getUid();
                                db.collection("users").document(uid)
                                        .update("location", locationText);

                                Toast.makeText(this, "Position sauvegardée ✅",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            String coords = "📍 " + location.getLatitude()
                                    + ", " + location.getLongitude();
                            tvLocation.setText(coords);
                        }
                    } else {
                        tvLocation.setText("📍 Position non disponible");
                        Toast.makeText(this, "Active le GPS sur ton téléphone",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 📸 Dialog choix photo
    private void showPhotoDialog() {
        String[] options = {"📷 Prendre une photo", "🖼️ Choisir depuis la galerie"};
        new AlertDialog.Builder(this)
                .setTitle("Photo de profil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    // 📷 Ouvrir caméra
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();
        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        }
    }

    // 🖼️ Ouvrir galerie
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // 📁 Créer fichier image temporaire
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        } catch (IOException e) {
            return null;
        }
    }

    // 💾 Sauvegarder profil
    private void saveProfile() {
        String uid  = mAuth.getCurrentUser().getUid();
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Entrez votre nom");
            return;
        }

        if (selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver()
                        .openInputStream(selectedImageUri);
                Bitmap bitmap  = BitmapFactory.decodeStream(inputStream);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                String base64Image = Base64.encodeToString(
                        baos.toByteArray(), Base64.DEFAULT);

                updateFirestore(uid, name, base64Image);

            } catch (Exception e) {
                Toast.makeText(this, "Erreur image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            updateFirestore(uid, name, null);
        }
    }

    // 🔄 Mettre à jour Firestore
    private void updateFirestore(String uid, String name, String base64Image) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (base64Image != null) updates.put("photoBase64", base64Image);

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(a ->
                        Toast.makeText(this, "Profil mis à jour ✅",
                                Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}