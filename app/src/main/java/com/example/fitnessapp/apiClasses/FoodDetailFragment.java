package com.example.fitnessapp.apiClasses;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.fitnessapp.Fragments.HomeFragment;
import com.example.fitnessapp.Fragments.ProgressFragment;
import com.example.fitnessapp.R;
import com.example.fitnessapp.foodFragments.FoodFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodDetailFragment extends Fragment {
    private static final String ARG_FOOD_ID = "foodId";
    private static final String ARG_FOOD_TITLE = "foodTitle";
    private static final String ARG_FOOD_IMAGE = "foodImage";
    private static final String ARG_FOOD_TYPE = "foodType";
    private static final String ARG_IS_UPDATE = "isUpdate";
    private static final String ARG_DOC_ID = "doc_id";

    private String foodType, userMail, foodTitle, imageURL, weightText, docId;
    private int foodId;
    private double servingSize, originalCalories, originalProteins, originalFats, originalCarbs;
    private boolean isUpdate;

    private TextView foodNameTextView, caloriesTextView, proteinTextView, fatsTextView, carbsTextView;
    private TextInputEditText weightEditText;
    private Button addFoodButton, updateFoodBtn, removeFoodBtn;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private ApiService apiService;

    private final String API_KEY = "9243c5262b7e4100b3efb98559ba228f";

    public static FoodDetailFragment newInstance(int foodId, String foodTitle, String imageURL, String foodType, boolean isUpdate, String docId) {
        FoodDetailFragment fragment = new FoodDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FOOD_ID, foodId);
        args.putString(ARG_FOOD_TITLE, foodTitle);
        args.putString(ARG_FOOD_IMAGE, imageURL);
        args.putString(ARG_FOOD_TYPE, foodType);
        args.putBoolean(ARG_IS_UPDATE, isUpdate);
        args.putString(ARG_DOC_ID, docId);
        fragment.setArguments(args);
        return fragment;
    }

    public static FoodDetailFragment newInstance(int foodId, String foodTitle, double calories, double proteins, double fats, double carbs, double servingSize, String docId, String foodType, boolean isUpdate) {
        FoodDetailFragment fragment = new FoodDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FOOD_ID, foodId);
        args.putString(ARG_FOOD_TITLE, foodTitle);
        args.putDouble("calories", calories);
        args.putDouble("proteins", proteins);
        args.putDouble("fats", fats);
        args.putDouble("carbs", carbs);
        args.putDouble("servingSize", servingSize);
        args.putString(ARG_DOC_ID, docId);
        args.putString(ARG_FOOD_TYPE, foodType);
        args.putBoolean(ARG_IS_UPDATE, isUpdate);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        apiService = RetrofitClient.getClient().create(ApiService.class);

        if (getArguments() != null) {
            foodId = getArguments().getInt(ARG_FOOD_ID);
            foodTitle = getArguments().getString(ARG_FOOD_TITLE);
            imageURL = getArguments().getString(ARG_FOOD_IMAGE);
            foodType = getArguments().getString(ARG_FOOD_TYPE);
            isUpdate = getArguments().getBoolean(ARG_IS_UPDATE);
            docId = getArguments().getString(ARG_DOC_ID);

            if (isUpdate && getArguments().containsKey("calories")) {
                originalCalories = getArguments().getDouble("calories");
                originalProteins = getArguments().getDouble("proteins");
                originalFats = getArguments().getDouble("fats");
                originalCarbs = getArguments().getDouble("carbs");
                servingSize = getArguments().getDouble("servingSize");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_food_detail, container, false);
        initComponents(view);

        foodNameTextView.setText(foodTitle);
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        if (isUpdate) {
            displayFoodInfo();
        } else {
            fetchFoodDetailsFromApi();
        }

        weightEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateNutrientValuesBasedOnWeight();
            }
        });

        return view;
    }

    private void initComponents(View view) {
        foodNameTextView = view.findViewById(R.id.foodNameTextView);
        weightEditText = view.findViewById(R.id.weightEditText);
        caloriesTextView = view.findViewById(R.id.caloriesTextView);
        proteinTextView = view.findViewById(R.id.proteinTextView);
        fatsTextView = view.findViewById(R.id.fatsTextView);
        carbsTextView = view.findViewById(R.id.carbsTextView);
        addFoodButton = view.findViewById(R.id.addBreakfastButton);
        backButton = view.findViewById(R.id.backImageButton);
        updateFoodBtn = view.findViewById(R.id.update_food_btn);
        removeFoodBtn = view.findViewById(R.id.remove_food_btn);
    }

    private void displayFoodInfo() {
        weightEditText.setText(String.format(Locale.getDefault(), "%.0f", servingSize));
        caloriesTextView.setText(getString(R.string.add_calories, originalCalories));
        proteinTextView.setText(getString(R.string.add_proteins, originalProteins));
        fatsTextView.setText(getString(R.string.add_fats, originalFats));
        carbsTextView.setText(getString(R.string.add_carbohydrates, originalCarbs));
        updateNutrientValuesBasedOnWeight();
    }

    private void fetchFoodDetailsFromApi() {
        apiService.getFoodNutrients(foodId, API_KEY).enqueue(new Callback<NutrientsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NutrientsResponse> call, @NonNull Response<NutrientsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Nutrients nutrients = response.body().toNutrients();

                    servingSize = nutrients.getServingSize();
                    originalCalories = nutrients.getCalories();
                    originalProteins = nutrients.getProteins();
                    originalCarbs = nutrients.getCarbs();
                    originalFats = nutrients.getFats();

                    weightEditText.setText(String.format(Locale.getDefault(), "%.0f", servingSize));
                    caloriesTextView.setText(getString(R.string.add_calories, originalCalories));
                    proteinTextView.setText(getString(R.string.add_proteins, originalProteins));
                    fatsTextView.setText(getString(R.string.add_fats, originalFats));
                    carbsTextView.setText(getString(R.string.add_carbohydrates, originalCarbs));

                    updateNutrientValuesBasedOnWeight();
                } else {
                    Toast.makeText(getContext(), "Failed to fetch food details", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NutrientsResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "API error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNutrientValuesBasedOnWeight() {
        weightText = weightEditText.getText().toString().trim();
        if (weightText.isEmpty()) return;

        try {
            double newServingSize = Double.parseDouble(weightText);
            if (newServingSize <= 0) return;

            double newCalories = (newServingSize * originalCalories) / servingSize;
            double newProteins = (newServingSize * originalProteins) / servingSize;
            double newFats = (newServingSize * originalFats) / servingSize;
            double newCarbs = (newServingSize * originalCarbs) / servingSize;

            caloriesTextView.setText(getString(R.string.add_calories, newCalories));
            proteinTextView.setText(getString(R.string.add_proteins, newProteins));
            fatsTextView.setText(getString(R.string.add_fats, newFats));
            carbsTextView.setText(getString(R.string.add_carbohydrates, newCarbs));

            if (isUpdate) {
                showUpdateButtons(newCalories, newProteins, newFats, newCarbs, newServingSize);
            } else {
                showAddButton(newCalories, newProteins, newFats, newCarbs, newServingSize);
            }

        } catch (NumberFormatException e) {
            Log.e("FoodDetailFragment", "Invalid weight input", e);
        }
    }

    private void showUpdateButtons(double cals, double prots, double fats, double carbs, double qty) {
        updateFoodBtn.setVisibility(View.VISIBLE);
        removeFoodBtn.setVisibility(View.VISIBLE);
        addFoodButton.setVisibility(View.GONE);
        updateFoodBtn.setOnClickListener(v -> updateFirestore(cals, prots, fats, carbs, qty));
        removeFoodBtn.setOnClickListener(v -> removeFoodFromFirestore());
    }

    private void showAddButton(double cals, double prots, double fats, double carbs, double qty) {
        updateFoodBtn.setVisibility(View.GONE);
        removeFoodBtn.setVisibility(View.GONE);
        addFoodButton.setVisibility(View.VISIBLE);
        addFoodButton.setOnClickListener(v -> saveInfoToFirestore(cals, prots, fats, carbs, qty, imageURL));
    }

    private void saveInfoToFirestore(double calories, double proteins, double fats, double carbs, double qty, String imageURL) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserSharedPref", getActivity().MODE_PRIVATE);
        userMail = sharedPreferences.getString("UserMail", null);
        if (userMail == null) return;

        Map<String, Object> foodData = new HashMap<>();
        foodData.put("userMail", userMail);
        foodData.put("idFood", foodId);
        foodData.put("foodName", foodTitle);
        foodData.put("foodImage", imageURL);
        foodData.put("calories", calories);
        foodData.put("proteins", proteins);
        foodData.put("fats", fats);
        foodData.put("carbohydrates", carbs);
        foodData.put("quantity", qty);
        foodData.put("foodType", foodType);
        foodData.put("date", new Timestamp(new Date()));

        db.collection("AddedFood")
                .add(foodData)
                .addOnSuccessListener(documentReference -> navigateToFragment())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving food", Toast.LENGTH_SHORT).show());
    }

    private void updateFirestore(double calories, double proteins, double fats, double carbs, double qty) {
        if (docId == null || docId.isEmpty()) return;

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("calories", calories);
        updateData.put("proteins", proteins);
        updateData.put("fats", fats);
        updateData.put("carbohydrates", carbs);
        updateData.put("quantity", qty);

        db.collection("AddedFood").document(docId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> navigateToFragment())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating food", Toast.LENGTH_SHORT).show());
    }

    private void removeFoodFromFirestore() {
        if (docId == null || docId.isEmpty()) return;

        db.collection("AddedFood").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> navigateToFragment())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error removing food", Toast.LENGTH_SHORT).show());
    }

    private void navigateToFragment() {
        refreshHome();
        //refreshProgress();
        FoodFragment foodFragment = FoodFragment.newInstance(foodType);
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, foodFragment);
        transaction.remove(FoodDetailFragment.this);
        transaction.commit();
    }

    private void refreshProgress(){
        Fragment progress = requireActivity().getSupportFragmentManager().findFragmentByTag("Progress");
        if(progress instanceof ProgressFragment){
            ((ProgressFragment) progress).getMacrosDB();
        }
    }

    private void refreshHome() {
        Fragment home = requireActivity().getSupportFragmentManager().findFragmentByTag("Home");
        if (home instanceof HomeFragment) {
            ((HomeFragment) home).retrieveDailyNutrients();
        }
    }
}