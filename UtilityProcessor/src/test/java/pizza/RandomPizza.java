package pizza;

import com.freenote.annotations.Factory;

@Factory(id = "Random", type = Meal.class)
public class RandomPizza implements Meal {
    @Override
    public float getPrice() {
        return 5.2f;
    }
}
