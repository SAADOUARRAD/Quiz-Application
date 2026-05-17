package com.example.quiz_application;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class quiz1 extends AppCompatActivity {

    RadioGroup radioGroup;
    RadioButton rb;
    Button button2, btnMic;
    TextView tvVoiceResult;

    String CorrectResp = "Neil Armstrong";
    String question = "Qui est le premier homme à marcher sur la Lune ? "
            + "Les choix sont : Yuri Gagarine. Neil Armstrong. Buzz Aldrin.";
    int score = 0;

    // Text to Speech
    TextToSpeech tts;

    // Launcher reconnaissance vocale
    ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    if (matches != null && !matches.isEmpty()) {
                        String voiceText = matches.get(0);
                        tvVoiceResult.setText("🎤 Vous avez dit : " + voiceText);

                        if (voiceText.toLowerCase().contains("neil") ||
                                voiceText.toLowerCase().contains("armstrong")) {
                            score += 1;
                            Toast.makeText(this, "✅ Bonne réponse !",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "❌ Mauvaise réponse ! : " + CorrectResp,
                                    Toast.LENGTH_SHORT).show();
                        }

                        Intent i1 = new Intent(quiz1.this, quiz2.class);
                        i1.putExtra("score", score);
                        startActivity(i1);
                        finish();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz1);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        radioGroup    = findViewById(R.id.radioGroup);
        button2       = findViewById(R.id.button2);
        btnMic        = findViewById(R.id.btnMic);
        tvVoiceResult = findViewById(R.id.tvVoiceResult);

        // ➕ Init TTS — lecture automatique au démarrage
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.FRENCH);
                tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // Bouton Next
        button2.setOnClickListener(v -> {
            if (radioGroup.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, "Choisissez une réponse SVP",
                        Toast.LENGTH_SHORT).show();
            } else {
                rb = findViewById(radioGroup.getCheckedRadioButtonId());
                if (rb.getText().toString().equals(CorrectResp)) {
                    score += 1;
                }
                Intent i1 = new Intent(quiz1.this, quiz2.class);
                i1.putExtra("score", score);
                startActivity(i1);
                finish();
            }
        });

        // Bouton Micro
        btnMic.setOnClickListener(v -> startVoiceRecognition());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites votre réponse...");
        try {
            voiceLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Reconnaissance vocale non disponible",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}