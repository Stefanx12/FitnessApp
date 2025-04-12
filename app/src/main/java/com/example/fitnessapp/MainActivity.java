package com.example.fitnessapp;

import static com.example.fitnessapp.Fragments.HomeFragment.getDailyCalories;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.fitnessapp.Fragments.HomeFragment;
import com.example.fitnessapp.Fragments.ProfileFragment;
import com.example.fitnessapp.Fragments.ProgressFragment;
import com.example.fitnessapp.authentification.UserActivity;
import com.example.fitnessapp.authentification.userinfo.ActivityLevelFragment;
import com.example.fitnessapp.authentification.userinfo.MeasurementsFragment;
import com.example.fitnessapp.authentification.userinfo.GoalFragment;
import com.example.fitnessapp.authentification.userinfo.NameFragment;
import com.example.fitnessapp.authentification.userinfo.ProgressRateFragment;
import com.example.fitnessapp.authentification.userinfo.QuizData;
import com.example.fitnessapp.authentification.authFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.SharedPreferences;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    FrameLayout frameLayout,quizLayout;
    private CardView cardView;
    private Button nextButton;
    private ImageButton returnButton;
    private int currentStep = 0;
    String selectedActivityLevel;
    SharedPreferences quizPreferences,userSharedPref;
    FirebaseFirestore db;
    HomeFragment homeFragment = new HomeFragment();
    ProgressFragment progressFragment = new ProgressFragment();
    private final List<Fragment> quizFragments = Arrays.asList(
            new NameFragment(),
            new GoalFragment(),
            new MeasurementsFragment(),
            new ProgressRateFragment(),
            new ActivityLevelFragment()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        quizPreferences = getSharedPreferences("QuizPreferences", MODE_PRIVATE);
        userSharedPref = getSharedPreferences("UserSharedPref",MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        frameLayout = findViewById(R.id.fragment_container);
        quizLayout = findViewById(R.id.quiz_fragment_container);
        nextButton = findViewById(R.id.go_next_button);
        returnButton = findViewById(R.id.return_img_button);
        cardView = findViewById(R.id.card_view_navigation);

        nextButton.setOnClickListener(v -> nextStep());
        returnButton.setOnClickListener(v -> previousStep());


        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                getSupportFragmentManager().beginTransaction()
                        .hide(progressFragment)
                        .show(homeFragment)
                        .commit();
            } else if (itemId == R.id.nav_progress) {
                Fragment progress = getSupportFragmentManager().findFragmentByTag("Progress");

                if (progress == null) {
                    progressFragment = new ProgressFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, progressFragment, "Progress")
                            .hide(homeFragment)
                            .commit();
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .hide(homeFragment)
                            .show(progress)
                            .commit();
                }
            }

            return true;
        });

        checkForUserMailInSharedPreferences();
        //resetUserSharedPreferences();
        //startQuiz();

//        boolean sign_up = getIntent().getBooleanExtra("sign_up", false);
//        boolean log_in = getIntent().getBooleanExtra("log_in", false);
//
//        //Log.d("MainActivity", "Received sign_up: " + sign_up + ", log_in: " + log_in);
//
//        if (sign_up) {
//            bottomNavigationView.setVisibility(View.GONE);
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.fragment_container,new authFragment())
//                    .commit();
//            //loadFragment(new authFragment());
//            Log.d("MainActivity", "Sign Up Fragment Loaded");
//        }
//
//        if (log_in) {
//            Log.d("MainActivity", "Log-In Pressed - Loading HomeFragment");
//            loadFragment(new HomeFragment());
//        }

        boolean openQuiz = getIntent().getBooleanExtra("quizFragment", false);
        Log.d("MainActivity","Open Quiz: " + openQuiz);
        if (openQuiz) {
            startQuiz();
        }
    }

    private void startQuiz(){
        resetGoalSharedPreferences();
        currentStep = 0;
        bottomNavigationView.setVisibility(View.GONE);
        frameLayout.setVisibility(View.GONE);
        quizLayout.setVisibility(View.VISIBLE);
        cardView.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        returnButton.setVisibility(View.VISIBLE);
        loadQuizFragment(quizFragments.get(currentStep));
    }

    private void nextStep() {
        Fragment currentFragment = quizFragments.get(currentStep);

        if (currentFragment instanceof QuizData) {
            String quizData = ((QuizData) currentFragment).getQuizData();
            if (quizData == null) {
                Toast.makeText(this, "Please complete the current step before proceeding.", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Validation failed for " + currentFragment.getClass().getSimpleName());
                return;
            }
        }

        currentStep++;

        // Verify the goal
        SharedPreferences sharedPreferences = getSharedPreferences("QuizPreferences", MODE_PRIVATE);
        String selectedGoal = sharedPreferences.getString("SelectedGoal", "");

        if ("Maintain Weight".equals(selectedGoal)) {
            // Skip ProgressRateFragment
            while (currentStep < quizFragments.size() && quizFragments.get(currentStep) instanceof ProgressRateFragment) {
                currentStep++;
            }
        }

        if (currentStep < quizFragments.size()) {
            loadQuizFragment(quizFragments.get(currentStep));
        } else {
            finishQuiz();
        }
    }

    private void previousStep() {
        if (currentStep > 0) {
            Fragment currentFragment = quizFragments.get(currentStep);
            if (currentFragment instanceof ProgressRateFragment) {
                ((ProgressRateFragment) currentFragment).resetWeeklyGoal();
            }

            currentStep--;

            for (Fragment fragment : quizFragments) {
                if (fragment instanceof GoalFragment) {
                    String selectedGoal = ((GoalFragment) fragment).getSelectedGoal();
                    if ("Maintain Weight".equals(selectedGoal)) {
                        while (currentStep > 0 && quizFragments.get(currentStep) instanceof ProgressRateFragment) {
                            currentStep--;
                        }
                    }
                    break;
                }
            }

            loadQuizFragment(quizFragments.get(currentStep));
        }
    }

    private void finishQuiz() {
        StringBuilder quizData = new StringBuilder();

        for (Fragment fragment : quizFragments) {
            if (fragment instanceof QuizData) {
                String data = ((QuizData) fragment).getQuizData();
                if (data != null) {
                    quizData.append(data).append("\n");
                } else {
                    Log.d("ProgressRateFragment","Fragment " + fragment.getClass().getSimpleName() + " returned null quiz data.");
                    Log.d("MainActivity", "Fragment " + fragment.getClass().getSimpleName() + " returned null quiz data.");
                }
            }
        }

        Log.d("QuizData", "Collected Quiz Data:\n" + quizData.toString());
        Log.d("ProgressRateFragment","Data: " + quizData.toString());

        calculateDailyIntake(quizPreferences);
        for(Fragment fragment : quizFragments){
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }
        addHomeFragment(new HomeFragment(),"Home");

        nextButton.setVisibility(View.GONE);
        returnButton.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        frameLayout.setVisibility(View.VISIBLE);
        cardView.setVisibility(View.GONE);
        quizLayout.setVisibility(View.GONE);
        insertUserIntoFirestore();
    }

    private void checkForUserMailInSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE);
        String userMail = sharedPreferences.getString("UserMail", null);
        boolean log_in = getIntent().getBooleanExtra("log_in", false);
        Log.d("MainActivity", "Checking UserMail in SharedPreferences: " + userMail);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if(userMail == null && !log_in){
            Intent intent = new Intent(MainActivity.this, UserActivity.class);
            intent.putExtra("from_Main", true);
            Log.d("MainActivity", "No user found, starting UserActivity");
            startActivity(intent);
            finish();
        }else if(log_in && user != null){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("UserMail",user.getEmail());
            editor.apply();
            addHomeFragment(homeFragment,"Home");
        }else{
            addHomeFragment(homeFragment,"Home");
        }
    }

    private void addHomeFragment(Fragment fragment,String tag){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container,fragment,tag)
                .commit();
    }

    public void changeFragment(Fragment current,Fragment target){
        getSupportFragmentManager().beginTransaction()
                .hide(current)
                .show(target)
                .commit();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean rememberMe = sharedPreferences.getBoolean("rememberMe",false);
        if(!rememberMe){
            editor.remove("UserMail");
        }
        editor.apply();
    }

    private void resetGoalSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("QuizPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.clear();
        editor.apply();
    }

    private void resetUserSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.clear();
        editor.apply();

        Log.d("MainActivity", "App reset to first launch state.");

        Intent intent = new Intent(MainActivity.this, UserActivity.class);
        startActivity(intent);
        finish();
    }

    public void deselectBottomNavItems() {
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
    }

    private void loadQuizFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.quiz_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    public String getSelectedActivityLevel() {
        return selectedActivityLevel;
    }

    public void disableBottomNav(){
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void enableBottomNav(){
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    public void setSelectedActivityLevel(String selectedActivityLevel) {
        this.selectedActivityLevel = selectedActivityLevel;
    }

    public void insertUserIntoFirestore(){
        Map<String,Object> userData = new HashMap<>();
        String userMail = userSharedPref.getString("UserMail","Guest").toLowerCase(Locale.ROOT);
        userData.put("ActivityLevel", quizPreferences.getString("ActivityLevel", "Moderate"));
        userData.put("Birthday", quizPreferences.getInt("Age", 0));
        userData.put("CaloriesGoal", Double.parseDouble(quizPreferences.getString("MaxCalories","0")));
        userData.put("CarbsGoal", Double.parseDouble(quizPreferences.getString("MaxCarbs","0")));
        userData.put("CurrentWeight", Integer.parseInt(quizPreferences.getString("CurrentWeight","0")));
        userData.put("FatsGoal", Double.parseDouble(quizPreferences.getString("MaxFats","0")));
        userData.put("FirstName", quizPreferences.getString("FirstName", "FirstName"));
        userData.put("Gender", quizPreferences.getString("Gender", "Male"));
        userData.put("Goal", quizPreferences.getString("SelectedGoal", "Maintain weight"));
        userData.put("Height", Integer.parseInt(quizPreferences.getString("Height","0")));
        userData.put("Progress", Double.parseDouble(quizPreferences.getString("Progress","0")));
        userData.put("ProteinsGoal", Double.parseDouble(quizPreferences.getString("MaxProteins","0")));
        userData.put("WeightGoal", Integer.parseInt(quizPreferences.getString("TargetWeight","0")));

        db.collection("Users")
                .document(userMail)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("MainActivity","User data added"))
                .addOnFailureListener(e -> {
                    Log.e("MainActivity","Error user data: " + e);
                });
    }

    public void calculateDailyIntake(SharedPreferences sharedPreferences) {
        String gender = sharedPreferences.getString("Gender", "Male");
        String goal = sharedPreferences.getString("SelectedGoal", "Maintain Weight");
        String activityLevel = sharedPreferences.getString("ActivityLevel", "Sedentary");
        double currentWeight = Double.parseDouble(sharedPreferences.getString("CurrentWeight", "70"));
        double targetWeight = Double.parseDouble(sharedPreferences.getString("TargetWeight", "70"));
        int height = Integer.parseInt(sharedPreferences.getString("Height", "175"));
        int age = sharedPreferences.getInt("Age", 25);
        String weeklyProgress = goal.equals("Maintain Weight") ? "0" : sharedPreferences.getString("Progress", "0");

        // Calculate daily calorie needs
        double dailyCalories = getDailyCalories(goal, weeklyProgress, gender, activityLevel, currentWeight, height, age);
        ;
        // Calculate macros
        double proteinCalories = dailyCalories * 0.25;
        double fatCalories = dailyCalories * 0.25;
        double carbCalories = dailyCalories * 0.50;

        double proteinGrams = proteinCalories / 4;
        double fatGrams = fatCalories / 9;
        double carbGrams = carbCalories / 4;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("MaxCalories", String.valueOf(dailyCalories));
        editor.putString("MaxProteins", String.valueOf(proteinGrams));
        editor.putString("MaxFats", String.valueOf(fatGrams));
        editor.putString("MaxCarbs", String.valueOf(carbGrams));
        editor.apply();

        Log.d("HomeFragment", "Daily Calories: " + dailyCalories);
        Log.d("HomeFragment", "Protein: " + proteinGrams + "g, Fat: " + fatGrams + "g, Carbs: " + carbGrams + "g");
    }
}