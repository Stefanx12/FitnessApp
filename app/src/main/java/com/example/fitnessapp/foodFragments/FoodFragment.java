package com.example.fitnessapp.foodFragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fitnessapp.Fragments.HomeFragment;
import com.example.fitnessapp.MainActivity;
import com.example.fitnessapp.R;
import com.example.fitnessapp.apiClasses.AlimentsAdapter;
import com.example.fitnessapp.apiClasses.ApiService;
import com.example.fitnessapp.apiClasses.FoodDetailFragment;
import com.example.fitnessapp.apiClasses.FoodSearchResponse;
import com.example.fitnessapp.apiClasses.RetrofitClient;
import com.example.fitnessapp.apiClasses.SearchFoodRecipe;
import com.example.fitnessapp.foodClasses.foodRecipe;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodFragment extends Fragment {
    private TextView titleTextView;
    private RecyclerView searchFoodRecycler,addedFoodRecycler;
    private ImageButton returnHome;
    private SearchView searchView;
    private AlimentsAdapter adapter;
    private ApiService apiService;
    private String mealType;
    private final String API_KEY = "0c7af53c79ca4e0a889cb1f422c08e8b";
    private FirebaseFirestore db;

    public static FoodFragment newInstance(String mealType){
        FoodFragment fragment = new FoodFragment();
        Bundle args = new Bundle();
        args.putString("mealType",mealType);
        fragment.setArguments(args);
        return fragment;
    }

    public FoodFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            mealType = getArguments().getString("mealType", "");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_food, container, false);


        initComponents(view);

        addedFoodRecycler.setVisibility(View.VISIBLE);

        searchFood();

        returnHome.setOnClickListener(v -> {
            ((MainActivity) getActivity()).enableBottomNav();
            ((MainActivity) requireActivity()).swipeHome(FoodFragment.this);
            //goHome(FoodFragment.this);
        });

        apiService = RetrofitClient.getClient().create(ApiService.class);
        loadFirestoreData();

        return view;
    }

    private void searchFood() {
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                addedFoodRecycler.setVisibility(View.GONE);
            } else {
                addedFoodRecycler.setVisibility(View.VISIBLE);
            }
        });
        // Set up the SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchFoods(query);
                hideKeyboard();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    new Handler().postDelayed(() -> {
                        hideKeyboard();
                        searchView.clearFocus();
                    }, 100);
                    if (adapter != null) {
                        adapter.clearList();
                    }
                }
                return false;
            }
        });
    }

    private void initComponents(View view) {
        titleTextView = view.findViewById(R.id.foodTitle);
        searchFoodRecycler = view.findViewById(R.id.search_food_recycler);
        addedFoodRecycler = view.findViewById(R.id.added_breakfast_recycler);
        searchView = view.findViewById(R.id.foodsearchView);
        returnHome = view.findViewById(R.id.backHomeImageButton);

        searchFoodRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        addedFoodRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        if(mealType.equals("breakfast")){
            titleTextView.setText("Breakfast");
            loadFirestoreData();
        } else if (mealType.equals("lunch")) {
            titleTextView.setText("Lunch");
            loadFirestoreData();
        } else if (mealType.equals("dinner")) {
            titleTextView.setText("Dinner");
            loadFirestoreData();
        } else if (mealType.equals("snack")) {
            titleTextView.setText("Snack");
            loadFirestoreData();
        }
    }

    private void goHome(Fragment fragment){
        Fragment home = requireActivity().getSupportFragmentManager().findFragmentByTag("Home");
        requireActivity().getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .show(home)
                .commit();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
    }

    private void searchFoods(String query) {
        apiService.searchFoods(query, 100, API_KEY).enqueue(new Callback<FoodSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<FoodSearchResponse> call, @NonNull Response<FoodSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SearchFoodRecipe> foods = response.body().getResults();
                    adapter = new AlimentsAdapter(foods, item -> {
                        if (item instanceof SearchFoodRecipe) {
                            SearchFoodRecipe food = (SearchFoodRecipe) item;
                            openFoodDetailFragment(food.getId(), food.getTitle(),food.getImage(),false);
                        } else if (item instanceof foodRecipe) {
                            foodRecipe food = (foodRecipe) item;
                        }
                    });

                    searchFoodRecycler.setAdapter(adapter);
                    searchFoodRecycler.setVisibility(View.VISIBLE);
                    Log.d("BreakfastFragment", "Search results displayed successfully.");
                } else {
                    Log.d("BreakfastFragment", "No results found for query: " + query);
                }
            }

            @Override
            public void onFailure(@NonNull Call<FoodSearchResponse> call, @NonNull Throwable t) {
                Log.e("BreakfastFragment", "Failed to fetch data from API", t);
            }
        });
    }

    public void loadFirestoreData() {
        // Current day start
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        // End of the day
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Timestamp endOfDay = new Timestamp(calendar.getTime());
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSharedPref",
                getActivity().MODE_PRIVATE);
        String userMail = sharedPreferences.getString("UserMail",null);

        if(userMail != null){
            // Fetch data from database with filters
            db.collection("AddedFood")
                    .whereEqualTo("foodType", mealType)
                    .whereEqualTo("userMail",userMail)
                    .whereGreaterThanOrEqualTo("date", startOfDay)
                    .whereLessThanOrEqualTo("date", endOfDay)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<foodRecipe> foods = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Map<String, Object> data = document.getData();

                                int idFood = parseToInt(data.get("idFood"));
                                double carbohydrates = parseToDouble(data.get("carbohydrates"));
                                double calories = parseToDouble(data.get("calories"));
                                double quantity = parseToDouble(data.get("quantity"));
                                double fats = parseToDouble(data.get("fats"));
                                double proteins = parseToDouble(data.get("proteins"));
                                String foodImage = (String) data.get("foodImage");

                                foodRecipe food = new foodRecipe(
                                        idFood,
                                        (String) data.get("foodName"),
                                        (String) data.get("foodType"),
                                        calories,
                                        quantity,
                                        proteins,
                                        fats,
                                        carbohydrates,
                                        (Timestamp) data.get("date"),
                                        foodImage
                                );

                                String docId = document.getId();

                                food.setDocumentId(docId);
                                foods.add(food);

                                Log.d("Update","ID: FoodFragment: " + docId);
                            } catch (Exception e) {
                                Log.e("FoodFragment", "Error deserializing document " + document.getId(), e);
                            }
                        }

                        adapter = new AlimentsAdapter(foods, item -> {
                            if (item instanceof foodRecipe) {
                                foodRecipe food = (foodRecipe) item;
                                openFoodDetailFragment(food.getIdFood(),food.getFoodName(), food.getCalories(), food.getProteins(), food.getFats(),
                                        food.getCarbohydrates(), food.getQuantity(),food.getDocId(),mealType,true);
                                Log.d("FoodFragment", "Clicked on food item: " + food.getFoodName() + " " + food.getCalories() + " " +
                                        food.getQuantity() + " " + food.getProteins() + " " + food.getFats() + " " + food.getCarbohydrates());
                                Log.d("Update","ID: FoodFragment: " + food.getDocId());
                            }
                        });

                        addedFoodRecycler.setAdapter(adapter);
                        Log.d("FoodFragment", "Firestore data loaded and displayed successfully.");
                    })
                    .addOnFailureListener(e -> Log.e("FoodFragment", "Failed to load data from Firestore", e));
        }
        else{
            Toast.makeText(getContext(), "Error reading user from database", Toast.LENGTH_SHORT).show();
            Log.d("FoodFragment","User Mail: " + userMail);
        }
    }

    private int parseToInt(Object value) {
        if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                Log.e("BreakfastFragment", "Failed to parse String to int: " + value, e);
            }
        }
        return 0;
    }

    public static double parseToDouble(Object value) {
        if (value instanceof Double) {
            return (double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                Log.e("BreakfastFragment", "Failed to parse String to double: " + value, e);
            }
        }
        return 0.0;
    }

    private void openFoodDetailFragment(int foodId,String foodTitle,double caloriesFire,double proteinsFire,double fatsFire,double carbsFire,double servingSizeFire,
                                           String docId,String mealType,boolean isUpdate) {
        FoodDetailFragment fragment = FoodDetailFragment.newInstance(foodId,foodTitle,caloriesFire,proteinsFire,fatsFire,carbsFire,servingSizeFire,
                docId,mealType,isUpdate);
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.remove(FoodFragment.this);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    private void openFoodDetailFragment(int foodId, String foodTitle,String imageURL,boolean isUpdate) {
        FoodDetailFragment fragment = FoodDetailFragment.newInstance(foodId, foodTitle,imageURL,mealType,isUpdate,"");
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.remove(FoodFragment.this);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}