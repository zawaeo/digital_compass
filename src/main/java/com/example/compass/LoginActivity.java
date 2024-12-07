package com.example.compass;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private EditText emailInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);

        loginButton.setOnClickListener(v -> attemptLogin());
        registerButton.setOnClickListener(v -> startActivity(
                new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (dbHelper.checkUser(email, password)) {
            // SharedPreferences
            SharedPreferences preferences = getSharedPreferences("CompassPrefs", Context.MODE_PRIVATE);
            preferences.edit().putString("user_email", email).apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }
}

