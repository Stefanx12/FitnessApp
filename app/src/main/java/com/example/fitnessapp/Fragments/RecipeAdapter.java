package com.example.fitnessapp.Fragments;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnessapp.R;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {
    private final List<String> steps;

    public RecipeAdapter(List<String> steps) {
        this.steps = steps;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stepText;

        public ViewHolder(View itemView) {
            super(itemView);
            stepText = itemView.findViewById(R.id.step_text);
        }
    }

    @NonNull
    @Override
    public RecipeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeAdapter.ViewHolder holder, int position) {
        holder.stepText.setText(steps.get(position));
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }
}