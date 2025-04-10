package com.example.fitnessapp.authentification.userinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fitnessapp.R;
import com.google.protobuf.StringValue;

public class ProgressRateFragment extends Fragment implements QuizData {
    private SeekBar goalSeekBar;
    private TextView goalWeight, recommendedGoal;
    private int weeklyGoal = 0;
    SharedPreferences sharedPreferences;

    public ProgressRateFragment() {
        // Required empty public constructor
    }

    public static ProgressRateFragment newInstance(String param1, String param2) {
        ProgressRateFragment fragment = new ProgressRateFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        sharedPreferences = getContext().getSharedPreferences("QuizPreferences",Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_progress_rate, container, false);

        initComponents(view);

        return view;
    }
    private void initComponents(View view) {
        goalSeekBar = view.findViewById(R.id.goal_seekBar);
        goalWeight = view.findViewById(R.id.weekly_goal_value_text_view);
        recommendedGoal = view.findViewById(R.id.recommened_message_goal_text_view);

        weeklyGoal = goalSeekBar.getProgress();
        goalWeight.setText(formatWeeklyGoal(weeklyGoal));

        Context context = getContext();
        if(context != null){
            String selectedGoal = sharedPreferences.getString("SelectedGoal", "");

            if ("MaintainWeight".equals(selectedGoal)) {
                goalSeekBar.setProgress(0);
                weeklyGoal = 0;
            }
            goalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    weeklyGoal = progress;
                    goalWeight.setText(formatWeeklyGoal(weeklyGoal));

                    if (weeklyGoal > 3 && weeklyGoal <= 6) {
                        recommendedGoal.setTextColor(getResources().getColor(R.color.white));
                    } else {
                        recommendedGoal.setTextColor(getResources().getColor(android.R.color.transparent));
                    }
                    Log.d("ProgressRateFragment","Weekly goal " + weeklyGoal);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            if ("MaintainWeight".equals(selectedGoal)) {
                goalSeekBar.setProgress(0);
                weeklyGoal = 0;
            }
        }else {
            Log.d("ProgressRateFragment","Intra pe context == null");
        }
    }
    @Override
    public String getQuizData() {
        if (sharedPreferences == null) {
            Log.e("ProgressRateFragment", "SharedPreferences is null.");
            return null;
        }

        if (!isValid()) {
            return null;
        }

        String selectedGoal = sharedPreferences.getString("SelectedGoal", "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!selectedGoal.equals("MaintainWeight")) {
            editor.putString("Progress", String.valueOf((weeklyGoal * 0.1)));
            editor.apply();
        }

        return "Weekly Goal: " + formatWeeklyGoal(weeklyGoal);
    }
    private boolean isValid() {
        if (weeklyGoal == 0) {
            Toast.makeText(getContext(), "Weekly goal cannot be 0. Please select a value.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    public static String formatWeeklyGoal(int progress) {
        double kgPerWeek = progress * 0.1;
        return String.format("%.1f kg/week", kgPerWeek);
    }
    public void resetWeeklyGoal() {
        weeklyGoal = 0;
        if (goalSeekBar != null) {
            goalSeekBar.setProgress(0);
        }
        if (goalWeight != null) {
            goalWeight.setText(formatWeeklyGoal(weeklyGoal));
        }
        Log.d("ProgressRateFragment", "Weekly goal and SeekBar reset to 0.");
    }

}