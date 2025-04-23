package com.example.fitnessapp.Fragments;

public class DailyMacro {
    public float carbs = 0f;
    public float fats = 0f;
    public float proteins = 0f;

    public float total() {
        return carbs + fats + proteins;
    }

    public float[] convertToProcentage() {
        float sum = total();
        if (sum == 0f) return new float[]{0f, 0f, 0f};
        return new float[]{
                carbs / sum * 100f,
                proteins / sum * 100f,
                fats / sum * 100f
        };
    }
}
