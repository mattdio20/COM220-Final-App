package com.example.android.com220finalapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.Date;

import service_and_storage.Meal;

public class Eating extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eating) ;

        final Button savebutton = (Button) findViewById(R.id.saveButton);
        savebutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save();
            }
        });

        final Button menubutton = (Button) findViewById(R.id.menuButton);
        menubutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                menu();
            }
        });

    }

    public void save()
    {
        RadioGroup radioButtonGroup = (RadioGroup) findViewById(R.id.radiogroup);
        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        Meal MealObj = new Meal();

        if(radioButtonID == 2131427427)
        {
            //set large
            //System.out.println("IN LARGE");
           MealObj.setMealType(Meal.MealType.Large);
        }
        if(radioButtonID == 2131427428)
        {
            //set medium
            //System.out.println("IN Medium");
            MealObj.setMealType(Meal.MealType.Medium);
        }
        if(radioButtonID == 2131427429)
        {
            //set small
            //System.out.println("IN small");
            MealObj.setMealType(Meal.MealType.Small);
        }

        EditText hourField = (EditText) findViewById(R.id.houramount);
        String hours = hourField.getText().toString();
        int hour = Integer.parseInt(hours);
        long timeEaten = new Date().getTime() - (hour * 60 * 60 * 1000);
        MealObj.setTimeEaten(timeEaten);

        //System.out.println(radioButtonID);
        //System.out.println(MealObj.getMealType());
        //System.out.println(MealObj.getTimeEaten());
    }

    public void menu()
    {
        Intent intent = new Intent(Eating.this, MainActivity.class);
        startActivity(intent);

    }


}
