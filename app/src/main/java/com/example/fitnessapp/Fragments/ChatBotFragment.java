package com.example.fitnessapp.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.fitnessapp.R;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatBotFragment extends Fragment {
    private final String AIML_API_KEY = "853bb58bfaa1495e8830a065c78447aa";
    private EditText ingredientsEditText;
    private TextView recipeTxtView;
    private Button ingredientsBtn;
    private RecyclerView recipeRecyclerView;
    private RecipeAdapter adapter;
    private List<String> words = new ArrayList<>();
    private static final String TAG = "ChatBotAI";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatbot_ai, container, false);

        ingredientsEditText = view.findViewById(R.id.ingredients_edit_text);
        ingredientsBtn = view.findViewById(R.id.get_recipe_btn);
        recipeTxtView = view.findViewById(R.id.recipe_txt_view);
        recipeRecyclerView = view.findViewById(R.id.recipe_recycler_view);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(words);
        recipeRecyclerView.setAdapter(adapter);

        ingredientsBtn.setOnClickListener(v -> {
            String ingredients = ingredientsEditText.getText().toString().trim();
            if (!ingredients.isEmpty()) {
                Log.d(TAG, "Ingrediente introduse: " + ingredients);
                getRecipeFromAI(ingredients);
                ingredientsEditText.setText("");
                ingredientsEditText.setHint("Enter ingredients");
            } else {
                recipeTxtView.setText("Te rog introdu câteva ingrediente!");
                Log.d(TAG, "Nu au fost introduse ingrediente.");
            }
        });

        return view;
    }

    private void displayRecipe(String fullText) {
        new Thread(() -> {
            StringBuilder currentText = new StringBuilder();

            for (char c : fullText.toCharArray()) {
                currentText.append(c);

                requireActivity().runOnUiThread(() -> {
                    if (words.isEmpty()) {
                        words.add(currentText.toString());
                        adapter.notifyItemInserted(0);
                    } else {
                        words.set(0, currentText.toString());
                        adapter.notifyItemChanged(0);
                    }
                    recipeRecyclerView.scrollToPosition(0);
                });

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getRecipeFromAI(String ingredients) {
        OkHttpClient client = new OkHttpClient();

        // DE VAZUT AICI DACA II DAU DOAR PT INGREDIENTE SAU ORICE

        String prompt = //"Sugerează o rețetă sănătoasă cu aceste ingrediente: " +
                 ingredients;
        String json = "{ \"model\": \"gpt-4o\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";

        Log.d(TAG, "Trimitem prompt: " + prompt);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://api.aimlapi.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + AIML_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Eroare rețea: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String res = response.body().string();
                        Log.d(TAG, "Răspuns primit: " + res);

                        JSONObject jsonObject = new JSONObject(res);
                        String recipe = jsonObject
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        requireActivity().runOnUiThread(() -> displayRecipe(recipe));
                        Log.d(TAG, "Rețetă extrasă: " + recipe);
                    } catch (Exception e) {
                        Log.e(TAG, "Eroare la parsare JSON: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Răspuns eșuat: " + response.message());
                }
            }
        });
    }
}