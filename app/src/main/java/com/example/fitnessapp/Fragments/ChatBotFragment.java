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
    private final String AIML_API_KEY = "secrey_key";
    private EditText ingredientsEditText;
    private Button ingredientsBtn;
    private RecyclerView recipeRecyclerView;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private static final String TAG = "ChatBotAI";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatbot_ai, container, false);

        ingredientsEditText = view.findViewById(R.id.ingredients_edit_text);
        ingredientsBtn = view.findViewById(R.id.get_recipe_btn);
        recipeRecyclerView = view.findViewById(R.id.recipe_recycler_view);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessageAdapter(messages);
        recipeRecyclerView.setAdapter(adapter);

        ingredientsBtn.setOnClickListener(v -> {
            String userText = ingredientsEditText.getText().toString().trim();
            if (!userText.isEmpty()) {
                ingredientsEditText.setText("");
                ingredientsEditText.setHint("Ask something...");

                // User message
                messages.add(new Message(userText, true));
                adapter.notifyItemInserted(messages.size() - 1);
                recipeRecyclerView.scrollToPosition(messages.size() - 1);

                // Bot message
                Message thinking = new Message("Thinking...", false);
                messages.add(thinking);
                int botIndex = messages.size() - 1;
                adapter.notifyItemInserted(botIndex);
                recipeRecyclerView.scrollToPosition(botIndex);

                getRecipeFromAI(userText, botIndex);
            } else {
                messages.add(new Message("Ask something.", false));
                adapter.notifyItemInserted(messages.size() - 1);
                recipeRecyclerView.scrollToPosition(messages.size() - 1);
                Log.d(TAG, "No input provided.");
            }
        });
        return view;
    }

    private void getRecipeFromAI(String prompt, int botIndex) {
        OkHttpClient client = new OkHttpClient();
        String json = "{ \"model\": \"gpt-4o\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://api.aimlapi.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + AIML_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    messages.set(botIndex, new Message("Something went wrong. Please try again.", false));
                    adapter.notifyItemChanged(botIndex);
                });
                Log.e(TAG, "Network error: " + e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String res = response.body().string();
                        JSONObject jsonObject = new JSONObject(res);
                        String reply = jsonObject
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        animateBotReply(reply, botIndex);
                    } catch (Exception e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "API failure: " + response.message());
                }
            }
        });
    }

    private void animateBotReply(String reply, int indexToReplace) {
        messages.remove(indexToReplace);
        adapter.notifyItemRemoved(indexToReplace);

        // Empty bot message for animation
        Message botMessage = new Message("", false);
        messages.add(botMessage);
        int newIndex = messages.size() - 1;
        adapter.notifyItemInserted(newIndex);

        new Thread(() -> {
            StringBuilder currentText = new StringBuilder();
            for (char c : reply.toCharArray()) {
                currentText.append(c);
                String partial = currentText.toString();

                requireActivity().runOnUiThread(() -> {
                    messages.get(newIndex).text = partial;
                    adapter.notifyItemChanged(newIndex);
                    recipeRecyclerView.scrollToPosition(newIndex);
                });

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
