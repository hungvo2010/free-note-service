package com.freenote.app.server.pizza;

import java.io.IOException;

public class PizzaStoreUsingFactory {
    private MealFactory factory = new MealFactory();

    public Meal order(String mealName) {
        return factory.create(mealName);
    }

    public static void main(String[] args) throws IOException {
        PizzaStoreUsingFactory pizzaStore = new PizzaStoreUsingFactory();
        Meal meal = pizzaStore.order("Margherita");
        System.out.println("Bill: $" + meal.getPrice());
    }
}
