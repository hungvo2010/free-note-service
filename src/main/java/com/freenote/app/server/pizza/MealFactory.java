package com.freenote.app.server.pizza;

import com.freenote.app.server.annotations.Tiramisu;

public class MealFactory {

    public Meal create(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null!");
        }
        if ("Calzone".equals(id)) {
            return new CalzonePizza();
        }

        if ("Tiramisu".equals(id)) {
            return new Tiramisu();
        }

        if ("Margherita".equals(id)) {
            return new MargheritaPizza();
        }

        throw new IllegalArgumentException("Unknown id = " + id);
    }
}
