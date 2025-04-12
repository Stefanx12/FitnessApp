package com.example.fitnessapp.Fragments;

import static com.example.fitnessapp.authentification.userinfo.ProgressRateFragment.formatWeeklyGoal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fitnessapp.MainActivity;
import com.example.fitnessapp.foodFragments.FoodFragment;
import com.example.fitnessapp.R;
import com.example.fitnessapp.authentification.UserActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    private TextView genderTxtView,activityTxtView,goalTxtView,userSavedWeight,userSavedHeight,userSavedAge,userSavedName;
    private SharedPreferences quizPreferences,userPreferences;
    private static final String PREF_KEY_WEIGHT = "CurrentWeight";
    private ImageButton returnHome;
    private Button signOut,registerGuest;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        quizPreferences = getContext().getSharedPreferences("QuizPreferences", Context.MODE_PRIVATE);
        userPreferences = requireContext().getSharedPreferences("UserSharedPref",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initComponents(view);
        checkForGuestUser();

        bottomSheetObjectiveChange();

        signOut.setOnClickListener(v -> signOutUser());

        returnHome.setOnClickListener(v ->{
            //MainActivity mainActivity = (MainActivity) getActivity();
            //mainActivity.switchToFragment(mainActivity.homeFragment);
            goHome(ProfileFragment.this);
            //loadHomeFragment();
            ((MainActivity) getActivity()).enableBottomNav();
        });

        userSavedObjective();

        return view;
    }

    private void goHome(Fragment fragment){
        Fragment home = requireActivity().getSupportFragmentManager().findFragmentByTag("Home");
        requireActivity().getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .show(home)
                //.add(R.id.fragment_container,new HomeFragment(),"Home") //(home)
                .commit();
        //((HomeFragment) home).refreshData();
    }

    private void signOutUser(){
        userPreferences.edit().clear().apply();
        quizPreferences.edit().clear().apply();
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void userSavedObjective() {
        String savedGender = quizPreferences.getString("Gender","");
        String savedWeight = quizPreferences.getString(PREF_KEY_WEIGHT, "");
        String savedHeight = quizPreferences.getString("Height","");
        int savedAge = quizPreferences.getInt("Age",0);
        String savedName = quizPreferences.getString("FirstName","");
        String savedActivity = quizPreferences.getString("ActivityLevel","");
        String savedGoal = quizPreferences.getString("SelectedGoal","");
        double savedProgress = Double.parseDouble(quizPreferences.getString("Progress",""));
        Log.d("Progress","Value of savedProgress" + savedProgress);

        userSavedWeight.setText(savedWeight + " kg");
        userSavedHeight.setText(savedHeight + " cm");
        userSavedAge.setText(savedAge + " years");
        userSavedName.setText(savedName);

        if(savedGoal.equals("Lose Weight")){
            goalTxtView.setText("Lose " + savedProgress + " kilograms per week");
        } else if (savedGoal.equals("Gain Weight")) {
            goalTxtView.setText("Gain " + savedProgress + " kilograms per week");
        }else{
            goalTxtView.setText("Maintain Weight");
        }
        activityTxtView.setText(savedActivity);
        genderTxtView.setText(savedGender);
    }

    private void initComponents(View view) {
        userSavedWeight = view.findViewById(R.id.user_saved_weight_txt_view);
        userSavedHeight = view.findViewById(R.id.user_saved_height_txt_view);
        userSavedAge = view.findViewById(R.id.user_saved_age_txt_view);
        userSavedName = view.findViewById(R.id.user_name_edit_txt);
        returnHome = view.findViewById(R.id.return_home_img_btn);
        genderTxtView = view.findViewById(R.id.gender_text_view);
        activityTxtView = view.findViewById(R.id.activity_lvl_txt_view);
        goalTxtView = view.findViewById(R.id.goal_text_view);
        signOut = view.findViewById(R.id.sign_out_user_btn);
        registerGuest = view.findViewById(R.id.register_guest_btn);
    }

    private void bottomSheetObjectiveChange() {
        List<String> genderOptions = Arrays.asList("Male","Female");
        List<String> activityOptions = Arrays.asList("Sedentary","Lightly Active","Moderately Active","Very Active","Super Active");
        List<String> blank = new ArrayList<>();

        genderTxtView.setOnClickListener(v -> BottomSheetObjective(genderOptions,genderTxtView,"Gender","Gender"));
        activityTxtView.setOnClickListener(v -> BottomSheetObjective(activityOptions,activityTxtView,"ActivityLevel","Activity level"));
        userSavedHeight.setOnClickListener(v ->BottomSheetObjective(blank,userSavedHeight,"Height","Height"));
        userSavedWeight.setOnClickListener(v -> BottomSheetObjective(blank,userSavedWeight,"CurrentWeight","Weight"));
        goalTxtView.setOnClickListener(v -> BottomSheetGoalChange(goalTxtView));
    }

    private void BottomSheetGoalChange(TextView textView){
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_goal,null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Make the bottom sheet full screen height
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
        bottomSheetDialog.show();

        CardView loseWeightCard = bottomSheetView.findViewById(R.id.lose_weight_card_view);
        CardView maintainWeightCard = bottomSheetView.findViewById(R.id.maintain_weight_card_view);
        CardView gainWeightCard = bottomSheetView.findViewById(R.id.gain_weight_card_view);
        Button saveBtn = bottomSheetView.findViewById(R.id.save_goal_btn);
        TextView progress = bottomSheetView.findViewById(R.id.progress_txt_view);
        SeekBar progressBar = bottomSheetView.findViewById(R.id.change_goal_seekBar);
        SharedPreferences.Editor editor = quizPreferences.edit();
        final String[] option = new String[1];

        loseWeightCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loseWeightCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.mint_green)));
                resetColor(maintainWeightCard);
                resetColor(gainWeightCard);
                progressBar.setVisibility(View.VISIBLE);
                progress.setVisibility(View.VISIBLE);
                option[0] = "Lose Weight";
            }
        });

        maintainWeightCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maintainWeightCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.mint_green)));
                resetColor(loseWeightCard);
                resetColor(gainWeightCard);
                progressBar.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                option[0] = "Maintain Weight";
            }
        });

        gainWeightCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gainWeightCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.mint_green)));
                resetColor(loseWeightCard);
                resetColor(maintainWeightCard);
                progressBar.setVisibility(View.VISIBLE);
                progress.setVisibility(View.VISIBLE);
                option[0] = "Gain Weight";
            }
        });

        final int[] progressValue = {0};

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("ProfileFragment","Progress value: " + progressValue[0] + " i: " + i);
                progressValue[0] = i;
                progress.setText(formatWeeklyGoal(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // optional
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // optional
            }
        });

        saveBtn.setOnClickListener(v -> {
            String goalText;
            if (option[0].equals("Maintain Weight")) {
                goalText = "Maintain Weight";
                int i = 0;
                editor.putString("SelectedGoal","Maintain Weight");
                editor.putString("Progress",String.valueOf(i));
                editor.apply();
            }else if(option[0].equals("Lose Weight")){
                String formattedValue = String.format(Locale.US, "%.1f", progressValue[0] * 0.1);
                goalText = "Lose " + formattedValue + " kilograms per week";
                editor.putString("SelectedGoal","Lose Weight");
                editor.putString("Progress", formattedValue);
                editor.apply();
            }else{
                String formattedValue = String.format(Locale.US, "%.1f", progressValue[0] * 0.1);
                goalText = "Gain " + formattedValue + " kilograms per week";
                editor.putString("SelectedGoal","Gain Weight");
                editor.putString("Progress", formattedValue);
                editor.apply();
            }
            textView.setText(goalText);

            refreshObjective();
            refreshHome();
            bottomSheetDialog.dismiss();
        });
    }
    private void resetColor(CardView cardView){
        cardView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
    }
    private void BottomSheetObjective(List<String> options,TextView textView,String quizKey,String title){
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_options,null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
        SharedPreferences.Editor editor = quizPreferences.edit();

        TextView titleTextView = bottomSheetView.findViewById(R.id.bottom_sheet_title);
        ListView optionsListView = bottomSheetView.findViewById(R.id.options_list);
        Button saveOption = bottomSheetView.findViewById(R.id.save_option_btn);
        TextInputEditText userEditTxt = bottomSheetView.findViewById(R.id.user_input_edit_txt);
        TextInputLayout layout = bottomSheetView.findViewById(R.id.user_input_layout);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, options);
        optionsListView.setAdapter(adapter);

        titleTextView.setText(title);

        changeWeightAndHeight(textView, quizKey, optionsListView, layout, userEditTxt, saveOption, editor, bottomSheetDialog);

        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String selectedOption = (String) adapterView.getItemAtPosition(position);

                saveOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editor.putString(quizKey,selectedOption);
                        editor.apply();

                        refreshObjective();
                        bottomSheetDialog.dismiss();
                        textView.setText(selectedOption);
                    }
                });
            }
        });
    }

    private void changeWeightAndHeight(TextView textView, String quizKey, ListView optionsListView, TextInputLayout layout, TextInputEditText userEditTxt, Button saveOption, SharedPreferences.Editor editor, BottomSheetDialog bottomSheetDialog) {
        if(textView.getId() == R.id.user_saved_height_txt_view){
            optionsListView.setVisibility(View.GONE);
            layout.setVisibility(View.VISIBLE);
            userEditTxt.setVisibility(View.VISIBLE);

            saveOption.setOnClickListener(v ->  {
                String height = userEditTxt.getText().toString().trim();
                editor.putString(quizKey,height);
                editor.apply();
                textView.setText(height + " cm");

                refreshObjective();

                bottomSheetDialog.dismiss();
            });
        }else if(textView.getId() == R.id.user_saved_weight_txt_view){
            optionsListView.setVisibility(View.GONE);
            layout.setVisibility(View.VISIBLE);
            layout.setHint("Write weight (kg)");
            userEditTxt.setVisibility(View.VISIBLE);

            saveOption.setOnClickListener(v ->  {
                String weight = userEditTxt.getText().toString().trim();
                Log.d("ProfileFragment","Weight: " + weight);
                editor.putString(quizKey,weight);
                editor.apply();
                textView.setText(weight + " kg");

                refreshObjective();
                refreshHome();

                bottomSheetDialog.dismiss();
            });
        }
    }

    private void refreshHome() {
        Fragment home = requireActivity().getSupportFragmentManager().findFragmentByTag("Home");
        ((HomeFragment) home).refreshData();
    }

    private void refreshObjective() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).calculateDailyIntake(quizPreferences);
            ((MainActivity) getActivity()).insertUserIntoFirestore();
        }
    }

    private void checkForGuestUser(){
        if(userPreferences.getString("UserMail","").endsWith("@guest.com")){
            registerGuest.setVisibility(View.VISIBLE);
            registerGuest.setOnClickListener(v -> showRegisterDialog());
        }
    }

    private void showRegisterDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_register_account, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        FrameLayout bottomSheet = bottomSheetDialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
        bottomSheetDialog.show();

        TextInputEditText emailEdit = bottomSheetView.findViewById(R.id.guest_user_email_input);
        TextInputEditText passwordEdit = bottomSheetView.findViewById(R.id.guest_user_pass_input);
        TextInputEditText confirmPasswordEdit = bottomSheetView.findViewById(R.id.guest_user_confirm_pass_input);
        Button registerBtn = bottomSheetView.findViewById(R.id.guest_user_register_account);

        registerBtn.setOnClickListener(v -> {
            String email = emailEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();
            String confirmPass = confirmPasswordEdit.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(getContext(), "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPass)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String oldEmail = userPreferences.getString("UserMail", "Guest");
                        updateFireStoreAndPreferences(oldEmail, email.toLowerCase(Locale.ROOT));
                        registerBtn.setVisibility(View.GONE);
                        bottomSheetDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileFragment", "Error registering guest: " + e.getMessage());
                        Toast.makeText(getContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void updateFireStoreAndPreferences(String oldEmail, String newEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Copy user data
        db.collection("Users").document(oldEmail).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                db.collection("Users").document(newEmail).set(document.getData())
                        .addOnSuccessListener(aVoid -> {
                            // Step 2: Migrate food entries
                            db.collection("AddedFood")
                                    .whereEqualTo("userMail", oldEmail)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        for (DocumentSnapshot doc : snapshot) {
                                            doc.getReference().update("userMail", newEmail);
                                        }
                                    });

                            // Step 3: Delete old guest user (optional)
                            db.collection("Users").document(oldEmail).delete();

                            // Step 4: Update SharedPreferences
                            SharedPreferences.Editor editor = userPreferences.edit();
                            editor.putString("UserMail", newEmail.toLowerCase(Locale.ROOT));
                            editor.apply();

                            Toast.makeText(getContext(), "Account created! You're now registered.", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> Log.e("ProfileFragment","Error update: " + e.getMessage()));
            }
        });
    }
}