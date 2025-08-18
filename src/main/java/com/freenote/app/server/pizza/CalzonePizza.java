package com.freenote.app.server.pizza;

import com.freenote.app.server.annotations.Factory;

@Factory(
        id = "Calzone",
        type = Meal.class
)
public class CalzonePizza implements Meal {
    @Override
    public float getPrice() {
        return 8.5f;
    }
}
