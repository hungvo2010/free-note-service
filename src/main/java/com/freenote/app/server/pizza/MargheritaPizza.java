package com.freenote.app.server.pizza;

import com.freenote.app.server.annotations.Factory;

@Factory(
        id = "Margherita",
        type = Meal.class
)
public class MargheritaPizza implements Meal {
    @Override
    public float getPrice() {
        return 6.0f;
    }
}
