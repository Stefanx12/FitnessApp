package com.example.fitnessapp.authentification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.credentials.GetCredentialRequest;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fitnessapp.MainActivity;
import com.example.fitnessapp.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.Map;

public class authFragment extends Fragment {
    private TextInputEditText emailTiet,passTiet,confirmPassTiet;
    private TextInputLayout confirmPassLayout;
    private TextView authMethods,forgotPassword;
    private Button authBtn;
    private ImageButton googleImgBtn,facebookImgBtn,returnImgBtn,showPass,showConfirmPass;
    LinearLayout linearLayout;
    private CheckBox rememberCheckBox;
    private boolean isSignUp,isVisible;
    SharedPreferences userSharedPref,quizPref;
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;
    public authFragment() {
        // Required empty public constructor
    }
    public static authFragment newInstance() {
        authFragment fragment = new authFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isSignUp = getArguments().getBoolean("isSignUp",true);
        }
        userSharedPref = requireContext().getSharedPreferences("UserSharedPref", Context.MODE_PRIVATE);
        quizPref = requireContext().getSharedPreferences("QuizPreferences",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_sign_up_, container, false);

        initComponents(view);

        manageInteractions();
        returnImgBtn.setOnClickListener(v -> returnToUserActivity());


//        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
//                .setFilterByAuthorizedAccounts(true)
//                .setServerClientId(getString(R.string.default_web_client_id))
//                .build();
//
//        GetCredentialRequest request = new GetCredentialRequest.Builder()
//                .addCredentialOption(googleIdOption)
//                .build();

        //forgotPassword.setOn

        googleImgBtn.setOnClickListener(v -> signInGoogle());
        forgotPassword.setOnClickListener(v -> forgotPassword());

        return view;
    }

    private void manageInteractions() {
        SharedPreferences.Editor editor = userSharedPref.edit();


        Log.d("SignUp","Valoare isSignUp " + isSignUp);

        rememberCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editor.putBoolean("rememberMe",b);
                editor.apply();
            }
        });

        isVisible = false;

        showPass.setOnClickListener(v ->{
            isVisible = !isVisible;
            togglePassVisibility(passTiet,isVisible);
        });

        showConfirmPass.setOnClickListener(v ->{
            isVisible = !isVisible;
            togglePassVisibility(confirmPassTiet,isVisible);
        });


        if(isSignUp){
            linearLayout.setVisibility(View.GONE);
            authBtn.setOnClickListener(v -> signUpUser());
        }else {
            confirmPassLayout.setVisibility(View.GONE);
            authMethods.setText("Log-In with");
            authBtn.setText("Log-In");
            authBtn.setOnClickListener(v -> LogInUser());
        }
    }

    private void initComponents(View view) {
        mAuth = FirebaseAuth.getInstance();
        emailTiet = view.findViewById(R.id.email_txt_input);
        passTiet = view.findViewById(R.id.pass_txt_input);
        confirmPassTiet = view.findViewById(R.id.pass_conf_txt_input);
        confirmPassLayout = view.findViewById(R.id.pass_confirm_txt_layout);
        authBtn = view.findViewById(R.id.sign_up_btn);
        googleImgBtn = view.findViewById(R.id.sign_up_google_img_btn);
        facebookImgBtn = view.findViewById(R.id.sign_up_facebook_img_btn);
        authMethods = view.findViewById(R.id.sign_up_methods_txt_view);
        linearLayout = view.findViewById(R.id.settings_log_in);
        returnImgBtn = view.findViewById(R.id.back_to_user_ImageButton);
        rememberCheckBox = view.findViewById(R.id.remember_me_checkBox);
        forgotPassword = view.findViewById(R.id.forgot_password);
        showPass = view.findViewById(R.id.show_pass_img_btn);
        showConfirmPass = view.findViewById(R.id.show_confirm_pass_img_btn);
    }

    private void togglePassVisibility(TextInputEditText editText, boolean visible) {
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        // Force refresh + keep cursor at end
        editText.setSelection(editText.getText().length());
    }

    private void forgotPassword(){
        String email = emailTiet.getText().toString().trim();

        if(email.isEmpty()){
            Toast.makeText(requireContext(), "Please enter your email !", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.sendPasswordResetEmail(email).
                addOnCompleteListener(task ->{
                    if(task.isSuccessful()){
                        Toast.makeText(getContext(), "Password reset email sent.", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(getContext(), "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                        Log.e("authFragment", "Error: ", task.getException());
                    }
                });
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),result ->{
                if(result.getResultCode() == getActivity().RESULT_OK && result.getData() != null){
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try{
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
                        mAuth.signInWithCredential(credential).addOnCompleteListener(getActivity(),authTask -> goToHome());
                    }catch (ApiException e){
                        Log.w("authFragment", "Google sign-in failed", e);
                    }
                }
            });
    private void signInGoogle(){
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }
    private void LogInUser(){
        String email = emailTiet.getText().toString().trim();
        String password = passTiet.getText().toString().trim();
        boolean rememberMe = userSharedPref.getBoolean("rememberMe",false);

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(getContext(), "Please enter email and password !", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(getActivity(),task ->{
           if(task.isSuccessful()){
               FirebaseUser user = mAuth.getCurrentUser();
               if(rememberMe){
                   saveUserToSharedPreferences(user.getEmail());
               }

               FirebaseFirestore db = FirebaseFirestore.getInstance();
               db.collection("Users").document(user.getEmail()).get().addOnSuccessListener(
                       documentSnapshot ->{
                           if(documentSnapshot.exists()){
                               SharedPreferences quizPref = requireContext().getSharedPreferences("QuizPreferences",Context.MODE_PRIVATE);
                               SharedPreferences.Editor editor = quizPref.edit();

                               for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                                   String key = entry.getKey();
                                   Object value = entry.getValue();

                                   if ("UserMail".equals(key)) {
                                       continue;
                                   }

                                   if ("Birthday".equals(key) && value instanceof Number) {
                                       editor.putInt("Age", Integer.parseInt(String.valueOf(value)));
                                   } else if ("Goal".equals(key)) {
                                       editor.putString("SelectedGoal",String.valueOf(value));
                                   } else if ("WeightGoal".equals(key)) {
                                       editor.putString("TargetWeight",String.valueOf(value));
                                   } else {
                                       editor.putString(key, String.valueOf(value));
                                   }
                               }
                               editor.apply();
                               goToHome();
                           }
                       }

               );
           }else {
               Toast.makeText(getContext(), "Email or password error", Toast.LENGTH_SHORT).show();
               Log.d("authFragment","Error: " + task.getException());
           }
        });
    }
    private void goToHome(){
        // MODIFICARE AICI
        requireActivity().getSupportFragmentManager().beginTransaction()
                .remove(authFragment.this).commit();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("log_in",true);
        startActivity(intent);
        getActivity().finish();
    }
    private void returnToUserActivity() {
        if (getActivity() instanceof UserActivity) {
            ((UserActivity) getActivity()).returnToUserActivity();
        }
    }
    private void signUpUser(){
        String email = emailTiet.getText().toString().trim();
        String password = passTiet.getText().toString().trim();
        String confirmPass = confirmPassTiet.getText().toString().trim();


        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Completează toate câmpurile!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            Toast.makeText(getContext(), "Parolele nu coincid!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "Parola trebuie să aibă cel puțin 6 caractere!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(getActivity(),task -> {
            if(task.isSuccessful()){
                FirebaseUser user = mAuth.getCurrentUser();
                if(user != null){
                    Log.d("authFragment","User: " + user.getEmail() + " ID : " + user);
                    saveUserToSharedPreferences(user.getEmail());
                    SharedPreferences.Editor editor = userSharedPref.edit();
                    editor.putBoolean("rememberMe",true);
                    editor.apply();
                    navigateToQuiz();
                }
            }else{
                if(task.getException() instanceof FirebaseAuthUserCollisionException){
                    Toast.makeText(getContext(), "Email already existent", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getContext(),task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("authFragment","Error: " + task.getException().getMessage());
                }
            }
        });
    }
    private void saveUserToSharedPreferences(String email) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSharedPref", getActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("UserMail", email);
        editor.apply();
    }
    private void navigateToQuiz() {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.putExtra("quizFragment", true);
            startActivity(intent);
            getActivity().finish();
    }
}