package com.example.fitnessapp.authentification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.fitnessapp.MainActivity;
import com.example.fitnessapp.R;

import java.util.Locale;
import java.util.UUID;

public class UserActivity extends AppCompatActivity {
    private Button guestBtn,loginButton,signUpButton;
    private TextView welcome,appName,message;
    private static FrameLayout authLayout;
    private String guestUserMail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initComponents();
    }

    private void initComponents() {
        guestBtn = findViewById(R.id.continue_as_guest_button);
        loginButton = findViewById(R.id.login_button);
        signUpButton = findViewById(R.id.sign_up_button);
        welcome = findViewById(R.id.login_welcome_message);
        appName = findViewById(R.id.login_welcome_message_2);
        message = findViewById(R.id.motivational_message_start);
        authLayout = findViewById(R.id.user_info_container);

        guestBtn.setOnClickListener(v -> generateGuestUser());
        signUpButton.setOnClickListener(v -> navToSignUp());
        loginButton.setOnClickListener(v -> navToLogIn());
    }

    public void returnToUserActivity() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        }

        welcome.setVisibility(View.VISIBLE);
        appName.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        signUpButton.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        guestBtn.setVisibility(View.VISIBLE);
    }

    private void generateGuestUser() {
        // Generate and save a new guest user
        guestUserMail = generateGuestMail();

        SharedPreferences sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("UserMail", guestUserMail.toLowerCase(Locale.ROOT));
        editor.putBoolean("rememberMe",true);
        editor.apply();

        Log.d("UserActivity", "Guest userMail created: " + guestUserMail);

        boolean fromMain = getIntent().getBooleanExtra("from_Main",false);

        Intent intent = new Intent(UserActivity.this, MainActivity.class);
        if(fromMain){
            intent.putExtra("quizFragment",true);
        }
        startActivity(intent);
        finish();
    }

    private String generateGuestMail() {
        return UUID.randomUUID().toString() + "@guest.com";
    }

    private void loadFragment(Fragment fragment){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.user_info_container,fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void changeUI(){
        welcome.setVisibility(View.GONE);
        appName.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        signUpButton.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        guestBtn.setVisibility(View.GONE);
    }

    private void navToSignUp(){
        authFragment fragment = new authFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("isSignUp",true);
        fragment.setArguments(bundle);
        changeUI();
        loadFragment(fragment);
    }

    private void navToLogIn(){
        authFragment fragment = new authFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("isSignUp",false);
        fragment.setArguments(bundle);
        changeUI();
        loadFragment(fragment);
    }
}