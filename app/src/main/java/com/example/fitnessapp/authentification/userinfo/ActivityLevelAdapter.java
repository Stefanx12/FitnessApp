package com.example.fitnessapp.authentification.userinfo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitnessapp.R;

import java.util.List;

public class ActivityLevelAdapter extends RecyclerView.Adapter<ActivityLevelAdapter.ViewHolder> {

    private final List<ActivityLevel> activityLevels;
    private final OnActivityLevelSelectedListener listener; // Callback for selection
    private int selectedPosition = -1; // To track the selected item

    public interface OnActivityLevelSelectedListener {
        void onActivityLevelSelected(String selectedLevel);
    }

    public ActivityLevelAdapter(List<ActivityLevel> activityLevels, OnActivityLevelSelectedListener listener) {
        this.activityLevels = activityLevels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_level_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityLevel activityLevel = activityLevels.get(position);

        holder.title.setText(activityLevel.getTitle());
        holder.subContext.setText(activityLevel.getDescription());
        holder.radioButton.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
            listener.onActivityLevelSelected(activityLevel.getTitle());
        });
        holder.radioButton.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
            listener.onActivityLevelSelected(activityLevel.getTitle());
        });
    }

    @Override
    public int getItemCount() {
        return activityLevels.size();
    }

    public String getSelectedActivityLevel() {
        return selectedPosition != -1 ? activityLevels.get(selectedPosition).getTitle() : null;
    }
    public void setSelectedItem(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subContext;
        RadioButton radioButton;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_text);
            subContext = itemView.findViewById(R.id.sub_context);
            radioButton = itemView.findViewById(R.id.radio_button);
        }
    }
}

