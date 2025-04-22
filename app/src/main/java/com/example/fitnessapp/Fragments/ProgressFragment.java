package com.example.fitnessapp.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fitnessapp.R;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends Fragment {

    private BarChart macrosBarChart, caloriesBarChart;
    private LineChart weightLineChart;
    private FirebaseFirestore db;
    private String userMail;
    private SharedPreferences userPref;
    private Handler dayCheckHandler = new Handler();
    private Runnable dayCheckRunnable;
    private int lastCheckedDay = -1;

    public ProgressFragment() {}

    public static ProgressFragment newInstance() {
        return new ProgressFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        userPref = requireActivity().getSharedPreferences("UserSharedPref", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);
        macrosBarChart = view.findViewById(R.id.macros_bar_chart);
        caloriesBarChart = view.findViewById(R.id.calories_bar_chart);
        weightLineChart = view.findViewById(R.id.weight_line_chart);
        userMail = userPref.getString("UserMail", null);

        getMacrosDB();
        getCaloriesDB();
        startCheckingForMonday();

        return view;
    }

    public void getMacrosDB() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date mondayOfThisWeek = calendar.getTime();

        if (userMail == null || userMail.isEmpty()) {
            userMail = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();
            Toast.makeText(requireContext(), "User email not found. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("AddedFood")
                .whereEqualTo("userMail", userMail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, DailyMacro> dailyMacros = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp("date");
                        if (timestamp == null) continue;

                        Date date = timestamp.toDate();
                        if (date.before(mondayOfThisWeek)) continue;

                        String dayKey = new SimpleDateFormat("EEE", Locale.ENGLISH).format(date);

                        float carbs = doc.contains("carbohydrates") ? doc.getDouble("carbohydrates").floatValue() : 0f;
                        float fats = doc.contains("fats") ? doc.getDouble("fats").floatValue() : 0f;
                        float proteins = doc.contains("proteins") ? doc.getDouble("proteins").floatValue() : 0f;

                        DailyMacro macros = dailyMacros.getOrDefault(dayKey, new DailyMacro());
                        macros.carbs += carbs;
                        macros.fats += fats;
                        macros.proteins += proteins;
                        dailyMacros.put(dayKey, macros);
                    }

                    AssignMacro assignMacros = getAssignMacro(dailyMacros);
                    showMacrosChart(assignMacros.filledEntries, assignMacros.emptyEntries, assignMacros.weekDays);
                });
    }

    public void getCaloriesDB() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date mondayOfThisWeek = calendar.getTime();

        if (userMail == null || userMail.isEmpty()) {
            userMail = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();
            Toast.makeText(requireContext(), "User email not found. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("AddedFood")
                .whereEqualTo("userMail", userMail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Float> dailyCalories = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp("date");
                        if (timestamp == null) continue;

                        Date date = timestamp.toDate();
                        if (date.before(mondayOfThisWeek)) continue;

                        String dayKey = new SimpleDateFormat("EEE", Locale.ENGLISH).format(date);

                        float calories = doc.contains("calories") ? doc.getDouble("calories").floatValue() : 0f;
                        float currentCalories = dailyCalories.getOrDefault(dayKey, 0f);
                        dailyCalories.put(dayKey, currentCalories + calories);
                    }

                    AssignCalories assignCalories = getAssignCalories(dailyCalories);
                    showCaloriesChart(assignCalories.filledEntries, assignCalories.emptyEntries, assignCalories.weekDays);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to fetch calories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @NonNull
    private static AssignMacro getAssignMacro(Map<String, DailyMacro> dailyMacros) {
        List<String> weekDays = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<BarEntry> filledBars = new ArrayList<>();
        List<BarEntry> emptyBars = new ArrayList<>();

        for (int i = 0; i < weekDays.size(); i++) {
            String day = weekDays.get(i);
            DailyMacro macros = dailyMacros.getOrDefault(day, new DailyMacro());

            if (macros != null && macros.total() > 0f) {
                filledBars.add(new BarEntry(i, macros.convertToProcentage()));
            } else {
                emptyBars.add(new BarEntry(i, new float[]{33f, 33f, 34f}));
            }
        }
        return new AssignMacro(weekDays, filledBars, emptyBars);
    }

    @NonNull
    private static AssignCalories getAssignCalories(Map<String, Float> dailyCalories) {
        List<String> weekDays = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < weekDays.size(); i++) {
            String day = weekDays.get(i);
            float calories = dailyCalories.getOrDefault(day, 0f);

            float caloriesPercent = 0f;

            if (HomeFragment.dailyCaloriesGoal > 0 && calories > 0) {
                caloriesPercent = (calories / HomeFragment.dailyCaloriesGoal) * 100f;
                caloriesPercent = Math.min(caloriesPercent, 120f); // cap at 120%
            }

            float filled = caloriesPercent;
            float empty = 120f - caloriesPercent;

            if (filled == 0f) {
                // Completely empty day, 0 filled, full gray
                entries.add(new BarEntry(i, new float[]{0f, 120f}));
            } else {
                entries.add(new BarEntry(i, new float[]{filled, empty}));
            }
        }
        return new AssignCalories(weekDays, entries, Collections.emptyList());
    }

    private void showMacrosChart(List<BarEntry> filledEntries, List<BarEntry> emptyEntries, List<String> dayLabels) {
        int colorProtein = Color.parseColor("#60A5FA");
        int colorFat = Color.parseColor("#FCD34D");
        int colorCarbs = Color.parseColor("#FB923C");
        int gray = Color.argb(80, 180, 180, 180);

        setupBarChart(macrosBarChart, filledEntries, emptyEntries, dayLabels,
                new int[]{colorProtein, colorFat, colorCarbs}, new String[]{"Protein", "Fat", "Carbs"}, gray, true);
    }

    private void showCaloriesChart(List<BarEntry> filledEntries, List<BarEntry> emptyEntries, List<String> dayLabels) {
        int colorCalories = Color.argb(255, 120, 200, 80);
        int colorEmpty = Color.argb(80, 180, 180, 180);
        //int colorEmpty = Color.parseColor("#A5A5A5");

        setupBarChart(
                caloriesBarChart,
                filledEntries,
                emptyEntries,
                dayLabels,
                new int[]{colorCalories, colorEmpty},
                new String[]{"Calories", ""},
                colorEmpty,
                false
        );
    }

    private void setupBarChart(BarChart chart, List<BarEntry> filledEntries, List<BarEntry> emptyEntries, List<String> dayLabels,
                               int[] filledColors, String[] filledLabels, int emptyColor, boolean showLegend) {

        BarDataSet filledSet = createDataSet(filledEntries, filledColors, filledLabels);
        BarDataSet emptySet = createDataSet(emptyEntries, new int[]{emptyColor}, new String[]{""});

        BarData barData = emptyEntries.isEmpty()
                ? new BarData(filledSet)
                : new BarData(filledSet, emptySet);
        barData.setBarWidth(0.5f);

        chart.setRenderer(new RoundedBarChartRenderer(chart, chart.getAnimator(), chart.getViewPortHandler()));
        chart.setData(barData);

        setupXAxis(chart, dayLabels);
        setupChartAppearance(chart);
        setupLegend(chart, filledColors, filledLabels, showLegend);

        chart.animateY(800, Easing.EaseInOutQuad);
        chart.invalidate();
    }

    private BarDataSet createDataSet(List<BarEntry> entries, int[] colors, String[] labels) {
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setStackLabels(labels);
        dataSet.setDrawValues(false);
        return dataSet;
    }

    private void setupXAxis(BarChart chart, List<String> labels) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
    }

    private void setupChartAppearance(BarChart chart) {
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setDescription(null);
        chart.setTouchEnabled(false);
        chart.setPinchZoom(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setExtraTopOffset(20f);
        chart.setExtraBottomOffset(12f);
        chart.setFitBars(true);
    }

    private void setupLegend(BarChart chart, int[] colors, String[] labels, boolean show) {
        Legend legend = chart.getLegend();
        legend.setEnabled(show);
        if (!show) return;
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);

        List<LegendEntry> entries = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            entries.add(new LegendEntry(labels[i], Legend.LegendForm.SQUARE, 10f, 2f, null, colors[i]));
        }
        legend.setCustom(entries);
    }

    private void startCheckingForMonday() {
        if (dayCheckRunnable != null) {
            dayCheckHandler.removeCallbacks(dayCheckRunnable);
        }

        dayCheckRunnable = () -> {
            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.DAY_OF_WEEK);

            if (today == Calendar.MONDAY && lastCheckedDay != Calendar.MONDAY) {
                macrosBarChart.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                    getMacrosDB();
                    macrosBarChart.animate().alpha(1f).setDuration(300);
                    Toast.makeText(requireContext(), "New week started!", Toast.LENGTH_SHORT).show();
                });
            }

            lastCheckedDay = today;
            dayCheckHandler.postDelayed(dayCheckRunnable, 60 * 1000);
        };

        dayCheckHandler.post(dayCheckRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dayCheckRunnable != null) {
            dayCheckHandler.removeCallbacks(dayCheckRunnable);
        }
    }

    // Helper classes
    private static class AssignMacro {
        public final List<String> weekDays;
        public final List<BarEntry> filledEntries;
        public final List<BarEntry> emptyEntries;

        public AssignMacro(List<String> weekDays, List<BarEntry> filledEntries, List<BarEntry> emptyEntries) {
            this.weekDays = weekDays;
            this.filledEntries = filledEntries;
            this.emptyEntries = emptyEntries;
        }
    }

    private static class AssignCalories {
        public final List<String> weekDays;
        public final List<BarEntry> filledEntries;
        public final List<BarEntry> emptyEntries;

        public AssignCalories(List<String> weekDays, List<BarEntry> filledEntries, List<BarEntry> emptyEntries) {
            this.weekDays = weekDays;
            this.filledEntries = filledEntries;
            this.emptyEntries = emptyEntries;
        }
    }
}