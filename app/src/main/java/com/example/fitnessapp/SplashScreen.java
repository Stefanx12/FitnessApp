package com.example.fitnessapp;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class SplashScreen extends Fragment {
    private ConstraintLayout splashScreen;
    private Drawable background;
    public SplashScreen(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_splash, container, false);


        splashScreen = view.findViewById(R.id.splash_container);
        background = splashScreen.getBackground();

        if(background instanceof TransitionDrawable){
            TransitionDrawable transitionDrawable = (TransitionDrawable) background;
            transitionDrawable.startTransition(2250);
        }

        return view;
    }
}
