package com.example.fitnessapp;

import static com.example.fitnessapp.Fragments.HomeFragment.getDailyCalories;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
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
import com.google.android.material.textfield.TextInputEditText;
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
    private boolean quizProgress = false;
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
                    ((ProgressFragment) progress).getMacrosDB();
                    ((ProgressFragment) progress).getCaloriesDB();
                    ((ProgressFragment) progress).getWeightHistory();
                }
            }

            return true;
        });

        quizProgress = quizPreferences.getBoolean("quiz_in_progress",false);
        if(quizProgress){
            startQuiz();
        }

        if (!getIntent().getBooleanExtra("fromSignOut", false)) {
            checkForUserMailInSharedPreferences();
        }
        //resetUserSharedPreferences();
        //startQuiz();

        boolean openQuiz = getIntent().getBooleanExtra("quizFragment", false);
        Log.d("MainActivity","Open Quiz: " + openQuiz);
        if (openQuiz) {
            startQuiz();
        }
    }

    private void startQuiz(){
        resetGoalSharedPreferences();
        quizPreferences.edit().putBoolean("quiz_in_progress",true).apply();
        currentStep = 0;
        bottomNavigationView.setVisibility(View.GONE);
        frameLayout.setVisibility(View.GONE);
        quizLayout.setVisibility(View.VISIBLE);
        cardView.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        returnButton.setVisibility(View.VISIBLE);
        loadQuizFragment(quizFragments.get(currentStep),true);
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
            loadQuizFragment(quizFragments.get(currentStep),true);
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

            loadQuizFragment(quizFragments.get(currentStep),false);
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

        quizPreferences.edit().putBoolean("quiz_in_progress",false).apply();
        nextButton.setVisibility(View.GONE);
        returnButton.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        frameLayout.setVisibility(View.VISIBLE);
        cardView.setVisibility(View.GONE);
        quizLayout.setVisibility(View.GONE);
        insertUserIntoFirestore();
        saveInitialWeightToHistory();
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
            //addSpashScreen();
            addHomeFragment(homeFragment,"Home");
        }else{
            addSpashScreen();
            //addHomeFragment(homeFragment,"Home");
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

        SharedPreferences.Editor editor = userSharedPref.edit();
        boolean rememberMe = userSharedPref.getBoolean("rememberMe",false);
        if(!rememberMe){
            resetUserSharedPreferences();
            //editor.remove("UserMail");
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
        quizPreferences.edit().clear().apply();
        userSharedPref.edit().clear().apply();

        Log.d("MainActivity", "App reset to first launch state.");

//        Intent intent = new Intent(MainActivity.this, UserActivity.class);
//        startActivity(intent);
//        finish();
    }

    public void deselectBottomNavItems() {
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
    }

    private void loadQuizFragment(Fragment fragment,boolean isNext) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if(isNext){
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left);
        }else {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left,R.anim.slide_out_right);
        }

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
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
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

    public void swipeHome(Fragment fragment){
        Fragment home = getSupportFragmentManager().findFragmentByTag("Home");
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .remove(fragment)
                .show(home)
                .commit();
    }

    public void enableSwipeToHome(View rootView,Fragment fragment) {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float deltaX = e2.getX() - e1.getX();
                float deltaY = Math.abs(e2.getY() - e1.getY());

                if (deltaX > 150 && deltaY < 100) {
                    swipeHome(fragment);
                    enableBottomNav();
                    return true;
                }
                return false;
            }
        });

        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void saveInitialWeightToHistory() {
        String userMail = userSharedPref.getString("UserMail", "Guest").toLowerCase(Locale.ROOT);
        double initialWeight = Double.parseDouble(quizPreferences.getString("CurrentWeight", "0"));

        Map<String, Object> weightEntry = new HashMap<>();
        weightEntry.put("userMail", userMail);
        weightEntry.put("weight", initialWeight);
        weightEntry.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("WeightHistory")
                .add(weightEntry)
                .addOnSuccessListener(documentReference -> Log.d("MainActivity", "Initial weight saved to history"))
                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to save initial weight: " + e.getMessage()));
    }

    public void addWeightToHistory(double newWeight) {
        String userMail = userSharedPref.getString("UserMail", "Guest").toLowerCase(Locale.ROOT);

        Map<String, Object> weightEntry = new HashMap<>();
        weightEntry.put("userMail", userMail);
        weightEntry.put("weight", newWeight);
        weightEntry.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("WeightHistory")
                .add(weightEntry)
                .addOnSuccessListener(documentReference -> Log.d("ProfileFragment", "New weight saved"))
                .addOnFailureListener(e -> Log.e("ProfileFragment", "Failed to save new weight: " + e.getMessage()));
    }

    private void addSpashScreen(){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container,new SplashScreen(),"Splash")
                .commit();
        disableBottomNav();
        new Handler(Looper.getMainLooper()).postDelayed(this::loadHomeFragment, 2500);
    }

    private void loadHomeFragment() {
        String userMail = userSharedPref.getString("UserMail", "Guest").toLowerCase(Locale.ROOT);

        db.collection("Users").document(userMail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, homeFragment, "Home")
                                    .commit();
                            enableBottomNav();
                        }, 1000);

                    } else {
                        Log.e("SplashScreen", "User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    Log.e("SplashScreen", "Firestore error: " + e.getMessage());
                });
    }
}