package io.gejsi.pufferfish.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityUserBinding;

public class UserActivity extends AppCompatActivity {
  private ActivityUserBinding binding;
  private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(new FirebaseAuthUIActivityResultContract(), this::onSignInResult);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityUserBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    updateUserDetails();
  }

  private void updateUserDetails() {
    ImageView userAvatarImageView = binding.userAvatar;
    TextView userNameTextView = binding.userName;
    TextView userEmailTextView = binding.userEmail;
    Button authButton = binding.authButton;

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    boolean isAuthed = currentUser != null;

    if (isAuthed) {
      String displayName = currentUser.getDisplayName();
      String email = currentUser.getEmail();
      String photoUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null;

      if (photoUrl != null) {
        Picasso.get().load(photoUrl).into(userAvatarImageView);
      } else {
        // Set a default placeholder image if no profile photo is available
        Picasso.get().load("https://t4.ftcdn.net/jpg/03/46/93/61/360_F_346936114_RaxE6OQogebgAWTalE1myseY1Hbb5qPM.jpg").into(userAvatarImageView);
      }

      userNameTextView.setText(displayName);
      userEmailTextView.setText(email);
      authButton.setText("Sign Out");
    } else {
      // Set a default placeholder image if user is a guest
      Picasso.get().load("https://t4.ftcdn.net/jpg/03/46/93/61/360_F_346936114_RaxE6OQogebgAWTalE1myseY1Hbb5qPM.jpg").into(userAvatarImageView);
      userNameTextView.setText("Guest");
      userEmailTextView.setText("You are a guest user.\nData will only be saved locally.");
      authButton.setText("Sign In");
    }

    authButton.setOnClickListener(v -> {
      if (isAuthed) {
        FirebaseAuth.getInstance().signOut();
        updateUserDetails();
      } else {
        createSignInIntent();
      }
    });
  }

  private void createSignInIntent() {
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build()
    );

    // Create and launch sign-in intent
    Intent signInIntent = AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.mipmap.ic_launcher_round)
            .setTheme(R.style.Theme_Pufferfish_NoActionBar)
            .build();
    signInLauncher.launch(signInIntent);
  }

  private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
    if (result.getResultCode() == RESULT_OK) {
      Intent intent = new Intent(this, UserActivity.class);
      startActivity(intent);
      this.finish();
    } else {
      Toast.makeText(this, "Sign in failed. Using Pufferfish as a guest.", Toast.LENGTH_SHORT).show();
    }
  }
}