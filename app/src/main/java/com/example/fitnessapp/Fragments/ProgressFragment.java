package com.example.fitnessapp.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.fitnessapp.R;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends Fragment {
    private BarChart macrosBarChart;
    private FirebaseFirestore db;
    private String userMail;
    private SharedPreferences userPref;

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
        userMail = userPref.getString("UserMail", "");

        db.collection("AddedFood")
                .whereEqualTo("userMail", userMail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, float[]> dailyMacros = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp("date");
                        if (timestamp == null) continue;

                        Date date = timestamp.toDate();
                        String dayKey = new SimpleDateFormat("EEE", Locale.getDefault()).format(date); // e.g. Mon

                        float carbs = doc.contains("carbohydrates") ? doc.getDouble("carbohydrates").floatValue() : 0f;
                        float fats = doc.contains("fats") ? doc.getDouble("fats").floatValue() : 0f;
                        float proteins = doc.contains("proteins") ? doc.getDouble("proteins").floatValue() : 0f;

                        float[] totals = dailyMacros.getOrDefault(dayKey, new float[]{0f, 0f, 0f});
                        totals[0] += carbs;
                        totals[1] += fats;
                        totals[2] += proteins;

                        dailyMacros.put(dayKey, totals);
                    }

                    // ⭐️ Define fixed week days (Sun to Sat or Mon to Sun depending on your preference)
                    List<String> weekDays = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

                    List<BarEntry> filledEntries = new ArrayList<>();
                    List<BarEntry> emptyEntries = new ArrayList<>();

                    for (int i = 0; i < weekDays.size(); i++) {
                        String day = weekDays.get(i);
                        float[] macros = dailyMacros.getOrDefault(day, new float[]{0f, 0f, 0f});
                        float total = macros[0] + macros[1] + macros[2];

                        if (total > 0f) {
                            float carbsPct = macros[0] / total * 100f;
                            float fatsPct = macros[1] / total * 100f;
                            float proteinsPct = macros[2] / total * 100f;
                            filledEntries.add(new BarEntry(i, new float[]{proteinsPct, fatsPct, carbsPct}));
                        } else {
                            // ⭐️ Dummy gray bar for empty day
                            emptyEntries.add(new BarEntry(i, new float[]{33f, 33f, 34f}));
                        }
                    }

                    showChart(filledEntries, emptyEntries, weekDays);
                });

        return view;
    }

    private void showChart(List<BarEntry> filledEntries, List<BarEntry> emptyEntries, List<String> dayLabels) {
        int colorProtein = Color.parseColor("#60A5FA");
        int colorFat = Color.parseColor("#FCD34D");
        int colorCarbs = Color.parseColor("#FB923C");

        // Filled dataset
        BarDataSet filledSet = new BarDataSet(filledEntries, "");
        filledSet.setColors(colorProtein, colorFat, colorCarbs);
        filledSet.setStackLabels(new String[]{"Protein", "Fat", "Carbs"});
        filledSet.setDrawValues(false);

        // ⭐️ Empty dataset (gray)
        int gray = Color.argb(80, 180, 180, 180);
        BarDataSet emptySet = new BarDataSet(emptyEntries, "");
        emptySet.setColors(gray, gray, gray);
        emptySet.setDrawValues(false);
        emptySet.setDrawIcons(false);
        emptySet.setStackLabels(new String[]{"", "", ""});

        BarData barData = emptyEntries.isEmpty()
                ? new BarData(filledSet)
                : new BarData(filledSet, emptySet);

        barData.setBarWidth(0.5f);
        macrosBarChart.setRenderer(new RoundedBarChartRenderer(
                macrosBarChart,
                macrosBarChart.getAnimator(),
                macrosBarChart.getViewPortHandler()
        ));

        macrosBarChart.setData(barData);

        // X-axis config
        XAxis xAxis = macrosBarChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));

        // Y-axis
        macrosBarChart.getAxisLeft().setEnabled(false);
        macrosBarChart.getAxisRight().setEnabled(false);

        // Appearance
        macrosBarChart.setDrawGridBackground(false);
        macrosBarChart.setDrawBorders(false);
        macrosBarChart.setDescription(null);
        macrosBarChart.setTouchEnabled(false);
        macrosBarChart.setPinchZoom(false);
        macrosBarChart.setHighlightPerTapEnabled(false);
        macrosBarChart.setExtraTopOffset(20f);
        macrosBarChart.setExtraBottomOffset(12f);
        macrosBarChart.setFitBars(true);

        // Legend
        Legend legend = macrosBarChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);
        legend.setCustom(Arrays.asList(
                new LegendEntry("Carbs", Legend.LegendForm.SQUARE, 10f, 2f, null, colorCarbs),
                new LegendEntry("Fat", Legend.LegendForm.SQUARE, 10f, 2f, null, colorFat),
                new LegendEntry("Protein", Legend.LegendForm.SQUARE, 10f, 2f, null, colorProtein)
        ));

        // Enable rounded bars


        // Animate and refresh
        macrosBarChart.animateY(800, Easing.EaseInOutQuad);
        macrosBarChart.invalidate();
    }
}
