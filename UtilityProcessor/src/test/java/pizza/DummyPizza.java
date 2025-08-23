package pizza;

import com.freenote.annotations.Factory;

@Factory(
        id = "Dummy",
        type = Meal.class
)
public class DummyPizza implements Meal {
    @Override
    public float getPrice() {
        return 1000.0f;
    }
}
