package com.example.fitnessapp.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.fitnessapp.MainActivity;
import com.example.fitnessapp.R;
import com.example.fitnessapp.foodFragments.FoodFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class HomeFragment extends Fragment {

    private ProgressBar progressBar,carbBar,proteinBar,fatsBar;
    private TextView progressText,fatValue,carbValue,proteinValue;
    private ImageButton profileButton,breakfastbutton,lunchbutton,dinnerbutton,snackbutton;
    private CardView breakfastCardView,lunchCardView,dinnerCardView,snackCardView;
    private int i = 0;
    private FirebaseFirestore db;
    SharedPreferences sharedPreferences,userPreferences;

    public HomeFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getContext().getSharedPreferences("QuizPreferences", Context.MODE_PRIVATE);
        userPreferences = getContext().getSharedPreferences("UserSharedPref",Context.MODE_PRIVATE);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initComponents(view);
        retrieveDailyNutrients();
        return view;
    }

    private void initComponents(View view){
        progressBar = view.findViewById(R.id.progress_bar);
        progressText = view.findViewById(R.id.progress_text);
        profileButton = view.findViewById(R.id.profileImageButton);
        carbValue = view.findViewById(R.id.carbvaluetext);
        breakfastbutton = view.findViewById(R.id.addbreakfastimgbutton);
        lunchbutton = view.findViewById(R.id.addlunchbutton);
        dinnerbutton = view.findViewById(R.id.adddinnerbutton);
        snackbutton = view.findViewById(R.id.addsnackbutton);
        proteinValue = view.findViewById(R.id.proteinValuetext);
        fatValue = view.findViewById(R.id.fatvaluetext);
        breakfastCardView = view.findViewById(R.id.breakfastcardview);
        lunchCardView = view.findViewById(R.id.lunchcardview);
        dinnerCardView = view.findViewById(R.id.dinnercardview);
        snackCardView = view.findViewById(R.id.snackcardview);
        carbBar = view.findViewById(R.id.CarbsprogressBar);
        proteinBar = view.findViewById(R.id.ProteinprogressBar);
        fatsBar = view.findViewById(R.id.FatssprogressBar);

        homeNavigations();
    }

    private void homeNavigations() {
        breakfastbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
                navToFood("breakfast");
            }
        });

        lunchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
                navToFood("lunch");
            }
        });

        dinnerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
                navToFood("dinner");
            }
        });

        snackbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
                navToFood("snack");
            }
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                changeFragment(new ProfileFragment());
//                requireActivity().getSupportFragmentManager().beginTransaction()
//                        .add(R.id.fragment_container,new ProfileFragment())
//                        .remove(HomeFragment.this)
//                        .commit();
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
            }
        });

        breakfastCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navToFood("breakfast");
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
            }
        });

        lunchCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navToFood("lunch");
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
            }
        });

        dinnerCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navToFood("dinner");
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
            }
        });

        snackCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                navToFood("snack");
                ((MainActivity) getActivity()).deselectBottomNavItems();
                ((MainActivity) getActivity()).disableBottomNav();
            }
        });
    }

    private void navToFood(String mealType) {
        FoodFragment foodFragment = FoodFragment.newInstance(mealType);
        Fragment home = requireActivity().getSupportFragmentManager().findFragmentByTag("Home");
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, foodFragment);
        transaction.hide(home);
        transaction.commit();
    }

    public void changeFragment(Fragment fragment){
        Fragment home = requireActivity().getSupportFragmentManager().findFragmentByTag("Home");

        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container,fragment)
                .hide(home)
                .commit();
    }

    public void retrieveDailyNutrients() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Timestamp endOfDay = new Timestamp(calendar.getTime());

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSharedPref",
                getActivity().MODE_PRIVATE);
        String userMail = sharedPreferences.getString("UserMail",null);
        Log.d("HomeFragment","User: " + userMail);
        if(userMail != null){
            db.collection("AddedFood")
                    .whereEqualTo("userMail",userMail)
                    .whereGreaterThanOrEqualTo("date", startOfDay)
                    .whereLessThanOrEqualTo("date", endOfDay)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        double totalCalories = 0;
                        double totalProteins = 0;
                        double totalFats = 0;
                        double totalCarbs = 0;

                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                totalCalories += document.getDouble("calories") != null ? document.getDouble("calories") :0;
                                totalProteins += document.getDouble("proteins") != null ? document.getDouble("proteins") : 0;
                                totalFats += document.getDouble("fats") != null ? document.getDouble("fats") : 0;
                                totalCarbs += document.getDouble("carbohydrates") != null ? document.getDouble("carbohydrates") : 0;
                            } catch (Exception e) {
                                Log.e("HomeFragment", "Error parsing nutrient data: " + e.getMessage());
                            }
                        }

                        updateNutrientValues(totalCalories,totalProteins, totalFats, totalCarbs);
                    })
                    .addOnFailureListener(e -> Log.e("HomeFragment", "Error fetching nutrient data: " + e.getMessage()));
        }
        else{
            Log.e("HomeFragment", "Error reading user in database");
        }
    }

    private void updateNutrientValues(double totalCalories,double totalProteins, double totalFats, double totalCarbs) {
        String userMail = userPreferences.getString("UserMail","Guest");

        if(userMail == null){
            Log.d("HomeFragment","User not found");
        }
        getMaxMacrosFromFirestore((int) totalCalories, (int) totalProteins, (int) totalFats, (int) totalCarbs, userMail);
    }

    private void getMaxMacrosFromFirestore(int totalCalories, int totalProteins, int totalFats, int totalCarbs, String userMail) {
        db.collection("Users")
                .document(userMail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve macros from db
                        int maxCalories = documentSnapshot.contains("CaloriesGoal") ?
                                documentSnapshot.getLong("CaloriesGoal").intValue() : 0;
                        int maxProteins = documentSnapshot.contains("ProteinsGoal") ?
                                documentSnapshot.getLong("ProteinsGoal").intValue() : 0;
                        int maxFats = documentSnapshot.contains("FatsGoal") ?
                                documentSnapshot.getLong("FatsGoal").intValue() : 0;
                        int maxCarbs = documentSnapshot.contains("CarbsGoal") ?
                                documentSnapshot.getLong("CarbsGoal").intValue() : 0;

                        // Update UI progress
                        progressBar.setMax(maxCalories);
                        progressBar.setProgress(totalCalories);
                        progressText.setText(totalCalories + " / " + maxCalories);

                        carbBar.setMax(maxCarbs);
                        carbBar.setProgress(totalCarbs);

                        proteinBar.setMax(maxProteins);
                        proteinBar.setProgress(totalProteins);

                        fatsBar.setMax(maxFats);
                        fatsBar.setProgress(totalFats);
                        setNutrientText(proteinValue, totalProteins, maxProteins);
                        setNutrientText(fatValue, totalFats, maxFats);
                        setNutrientText(carbValue, totalCarbs, maxCarbs);
                    } else {
                        Log.e("HomeFragment", "User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Internet connection failure !", Toast.LENGTH_LONG).show();
                    Log.e("HomeFragment", "Error fetching max values from Firestore: " + e.getMessage());
                });
    }

    private void setNutrientText(TextView textView, int currentValue, int maxValue) {
        String text = currentValue + " / " + maxValue + "g";
        textView.setText(text);
    }

    public static double getDailyCalories(String goal,String progress,String gender,String activityLevel,double kg,int height,int age){
        double RMB = 0;
        double maintananceCaloriesIntake = 0;
        double targetCalories = 0;
        if (gender.equals("Male")) {
            RMB = 10 * kg + 6.25 * height - 5 * age + 5;
        }
        else{
            RMB = 10 * kg + 6.25 * height - 5 * age - 161;
        }

        if(activityLevel.equals("Sedentary")){
            maintananceCaloriesIntake = RMB * 1.2;
        } else if (activityLevel.equals("Lightly Active")) {
            maintananceCaloriesIntake = RMB * 1.375;
        } else if (activityLevel.equals("Moderately Active")) {
            maintananceCaloriesIntake = RMB * 1.55;
        } else if (activityLevel.equals("Very Active")) {
            maintananceCaloriesIntake = RMB * 1.725;
        } else if (activityLevel.equals("Super Active")) {
            maintananceCaloriesIntake = RMB * 1.9;
        }

        if (goal.equalsIgnoreCase("Maintain Weight")) {
            targetCalories = maintananceCaloriesIntake;
        } else if (goal.equalsIgnoreCase("Lose Weight")) {
            double weightLossRate = Double.parseDouble(progress);
            double calorieDeficit = weightLossRate * 7700 / 7;
            targetCalories = maintananceCaloriesIntake - calorieDeficit;
        } else if (goal.equalsIgnoreCase("Gain Weight")) {
            double weightGainRate = Double.parseDouble(progress);
            double calorieSurplus = weightGainRate * 7700 / 7;
            targetCalories = maintananceCaloriesIntake + calorieSurplus;
        }


        return targetCalories;
    }

    public void refreshData(){
        retrieveDailyNutrients();
    }
}
