package com.example.fitnessapp.Fragments;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.core.content.ContextCompat;

import com.example.fitnessapp.R;

import java.util.List;

public class ProfileAdapter extends ArrayAdapter<String> {
    private int selectedPosition = -1;
    private final Context context;
    private final List<String> items;

    public ProfileAdapter(Context context, List<String> items) {
        super(context, android.R.layout.simple_list_item_1, items);
        this.context = context;
        this.items = items;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));

        if (position == selectedPosition) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.mint_green));
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        }
        return view;
    }
}

