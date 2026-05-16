package com.example.quiz_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    EditText name, Email, etPassword, etConfirmPassword;
    Button etRegister;

    FirebaseAuth myauth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        myauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        name = findViewById(R.id.name);
        Email = findViewById(R.id.Email);
        etPassword = findViewById(R.id.password);
        etConfirmPassword = findViewById(R.id.confirmPassword);
        etRegister = findViewById(R.id.button);

        etRegister.setOnClickListener(v -> {

            String userName = name.getText().toString().trim();
            String email = Email.getText().toString().trim();
            String motPass = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            // Vérifications
            if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(motPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(this, "Remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }
            if (motPass.length() < 6) {
                Toast.makeText(this, "Mot de passe : 6 caractères minimum",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!motPass.equals(confirmPass)) {
                Toast.makeText(this, "Mots de passe différents",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 Création compte Firebase Auth
            myauth.createUserWithEmailAndPassword(email, motPass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = myauth.getCurrentUser().getUid();

                            // ➕ Sauvegarder profil dans Firestore
                            Map<String, Object> user = new HashMap<>();
                            user.put("name", userName);
                            user.put("email", email);
                            user.put("score", 0);
                            user.put("photoBase64", "");
                            user.put("uid", uid);

                            db.collection("users")
                                    .document(uid)
                                    .set(user)
                                    .addOnSuccessListener(a -> {
                                        Toast.makeText(this, "Inscription réussie ✅",
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Erreur Firestore: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "Erreur: " +
                                            task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}