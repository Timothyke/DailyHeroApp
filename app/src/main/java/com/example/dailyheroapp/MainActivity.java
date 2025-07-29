package com.example.dailyheroapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private Button signInButton, signOutButton, btnAddTask, btnViewTasks;

    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔗 Link UI
        signInButton = findViewById(R.id.signInBtn);
        signOutButton = findViewById(R.id.signOutBtn);
        btnAddTask = findViewById(R.id.btnAddTask);
        btnViewTasks = findViewById(R.id.btnViewTasks);

        // 🔐 Firebase setup
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 🎯 Handle Google sign-in result
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                });

        // 🔘 Sign In
        signInButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });

        // 🚪 Sign Out
        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
                toggleTaskButtons(false);
                signInButton.setEnabled(true);
            });
        });

        // 📝 Add Task
        btnAddTask.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
        });

        // 📋 View Tasks
        btnViewTasks.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ViewTasksActivity.class));
        });

        toggleTaskButtons(false); // initially disabled
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Toast.makeText(this, "Welcome back " + currentUser.getDisplayName(), Toast.LENGTH_SHORT).show();
            toggleTaskButtons(true);
            signInButton.setEnabled(false);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(MainActivity.this, "Welcome " + user.getDisplayName(), Toast.LENGTH_LONG).show();
                toggleTaskButtons(true);
                signInButton.setEnabled(false);
            } else {
                Toast.makeText(MainActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleTaskButtons(boolean enabled) {
        btnAddTask.setEnabled(enabled);
        btnViewTasks.setEnabled(enabled);
        signOutButton.setEnabled(enabled);
    }
}
