package com.example.quiz_application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

public class Score extends AppCompatActivity {

    ProgressBar pb;
    TextView tvScore;
    Button btnTryAgain, btnLogout, btnProfile;

    int score;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_score);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pb          = findViewById(R.id.progressBar2);
        tvScore     = findViewById(R.id.Valeur_score);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        btnLogout   = findViewById(R.id.btpLogOut);
        btnProfile  = findViewById(R.id.btnProfile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Récupérer le score
        score = getIntent().getIntExtra("score", 0);

        // Calculer le pourcentage
        int percentage = (100 * score) / 5;
        tvScore.setText(percentage + " %");
        pb.setProgress(percentage);

        // Sauvegarder dans Firestore
        saveScore();

        // ➕ Envoyer score à FastAPI
        sendScoreToApi();

        // Bouton Try Again
        btnTryAgain.setOnClickListener(v -> {
            Toast.makeText(this, "Restart Quiz", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, quiz1.class));
            finish();
        });

        // Bouton Logout
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Merci pour votre participation",
                    Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Bouton Profil
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    // Sauvegarder le score dans Firestore
    private void saveScore() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long oldScore = doc.getLong("score");
                        if (oldScore == null || score > oldScore) {
                            db.collection("users").document(uid)
                                    .update("score", score);
                        }
                    }
                });
    }

    // ➕ Envoyer score à FastAPI
    private void sendScoreToApi() {
        if (mAuth.getCurrentUser() == null) return;

        new Thread(() -> {
            try {
                JSONObject scoreData = new JSONObject();
                scoreData.put("userId", mAuth.getCurrentUser().getUid());
                scoreData.put("nom", mAuth.getCurrentUser().getEmail());
                scoreData.put("score", score);

                String response = ApiClient.post("/scores", scoreData);

                runOnUiThread(() -> {
                    if (response != null) {
                        Toast.makeText(this, "Score envoyé à l'API ✅",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}