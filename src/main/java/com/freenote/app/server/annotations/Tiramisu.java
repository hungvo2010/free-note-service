package com.freenote.app.server.annotations;

import com.freenote.annotations.Factory;
import com.freenote.app.server.pizza.Meal;

@Factory(
        id = "Tiramisu",
        type = Meal.class
)
public class Tiramisu implements Meal {
    @Override
    public float getPrice() {
        return 4.5f;
    }
}
