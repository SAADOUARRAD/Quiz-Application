package com.example.quiz_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    EditText Email, password;
    Button button;
    TextView tvRegister;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Email = findViewById(R.id.Email);
        password = findViewById(R.id.password);
        button = findViewById(R.id.button);
        tvRegister = findViewById(R.id.textView);

        mAuth = FirebaseAuth.getInstance();

        button.setOnClickListener(v -> {
            String userEmail = Email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (userEmail.isEmpty()) {
                Email.setError("Entrez votre email");
                Email.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Email.setError("Email invalide");
                Email.requestFocus();
                return;
            }

            if (userPassword.isEmpty()) {
                password.setError("Entrez votre mot de passe");
                password.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this,
                                    "Connexion réussie",
                                    Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(MainActivity.this, quiz1.class));
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Votre Email ou Mot de Passe est incorrect",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {
            Intent i1 = new Intent(MainActivity.this, Register.class);
            startActivity(i1);
        });
    }
}