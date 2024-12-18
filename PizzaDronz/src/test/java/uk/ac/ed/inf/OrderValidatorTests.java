package uk.ac.ed.inf;
import org.junit.Test;
import uk.ac.ed.inf.Validation.OrderValidator;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.Assert.*;

public class OrderValidatorTests {
    Pizza[] pizzas = {new Pizza("Margarita", 1000), new Pizza("Calzone", 1400), new Pizza("Meat Lover", 1400),
        new Pizza("Vegan Delight", 1100), new Pizza( "Super Cheese", 1400), new Pizza("All Shrooms", 900)};

    Restaurant[] restaurants = {
            new Restaurant("Civerinos Slice", new LngLat(-3.1912869215011597, 55.945535152517735),
                    new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY}, Arrays.copyOfRange(pizzas,0,2)),
            new Restaurant("Sora Lella Vegan Restaurant",new LngLat(-3.202541470527649,55.943284737579376),
                    new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY},  Arrays.copyOfRange(pizzas,2,4) ),
            new Restaurant("Domino's Pizza - Edinburgh - Southside", new LngLat(-3.1838572025299072,55.94449876875712),
                    new DayOfWeek[]{DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY},Arrays.copyOfRange(pizzas,4,6))};

    OrderValidator testInstance = new OrderValidator();
    LocalDate orderDate = LocalDate.of(2023,10,6);
    Order testOrder = new Order("200", orderDate, OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED, 2500, Arrays.copyOfRange(pizzas,0,2),
            new CreditCardInformation("1111222233334444", "11/25", "919"));
    @Test
    public void validOrder(){
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(testOrder.getOrderValidationCode(),OrderValidationCode.NO_ERROR);
    }

    @Test
    public void invalidCardNum_Length(){
        testOrder.getCreditCardInformation().setCreditCardNumber("19456543098971111");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.CARD_NUMBER_INVALID, testOrder.getOrderValidationCode());
    }
    @Test
    public void InvalidCardNum_Characters(){
        testOrder.getCreditCardInformation().setCreditCardNumber("ei392r43jnr48754");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.CARD_NUMBER_INVALID, testOrder.getOrderValidationCode());
    }

    @Test
    public void invalidExpiry_Expired(){
        testOrder.getCreditCardInformation().setCreditCardExpiry("01/22");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.EXPIRY_DATE_INVALID, testOrder.getOrderValidationCode());
    }
    @Test
    public void invalidExpiry_Format(){
        testOrder.getCreditCardInformation().setCreditCardExpiry("1224");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.EXPIRY_DATE_INVALID, testOrder.getOrderValidationCode());
    }

    @Test
    public void invalidExpiry_Characters(){
        testOrder.getCreditCardInformation().setCreditCardExpiry("12c473");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.EXPIRY_DATE_INVALID, testOrder.getOrderValidationCode());
    }

    @Test
    public void invalidExpiry_CloseToValid(){
        LocalDate date = LocalDate.of(2023,10,1);
        testOrder.setOrderDate(date);
        testOrder.getCreditCardInformation().setCreditCardExpiry("09/23");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.EXPIRY_DATE_INVALID, testOrder.getOrderValidationCode());
    }
    @Test
    public void validExpiry_CloseToInvalid(){
        LocalDate date = LocalDate.of(2022, 10,31);
        testOrder.setOrderDate(date);
        testOrder.getCreditCardInformation().setCreditCardExpiry("10/22");
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.NO_ERROR, testOrder.getOrderValidationCode());
    }

    @Test
    public void invalidCVV_Length(){
        //Resetting other credit info
        testOrder.setCreditCardInformation(new CreditCardInformation("1111222233334444","11/25", "9100" ));
        testOrder.setOrderDate(orderDate);
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.CVV_INVALID, testOrder.getOrderValidationCode());
    }

    @Test
    public void invalidCVV_Characters(){
        testOrder.setCreditCardInformation(new CreditCardInformation("1111222233334444", "10/26", "3o3"));
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.CVV_INVALID, testOrder.getOrderValidationCode());
    }
    @Test
    public void pizzaExist(){
        testOrder.setPizzasInOrder(new Pizza[]{new Pizza("All Shrooms", 900)});
        testOrder.setPriceTotalInPence(1000);
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.NO_ERROR, testOrder.getOrderValidationCode());
    }

    @Test
    public void pizzaDoesntExist(){
        testOrder.setPizzasInOrder(new Pizza[]{new Pizza("All Shrooms", 900), new Pizza("Yummers", 130000)});
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.PIZZA_NOT_DEFINED, testOrder.getOrderValidationCode());
    }

    @Test
    public void pizzaDoesntExist_null(){
        testOrder.setPizzasInOrder(new Pizza[]{new Pizza("",0)});
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.PIZZA_NOT_DEFINED, testOrder.getOrderValidationCode());
    }

    @Test
    public void pizzasFromSameRestaraunt(){
        testOrder.setPizzasInOrder(Arrays.copyOfRange(pizzas,2,4));
        testOrder.setPriceTotalInPence(2600);
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.NO_ERROR, testOrder.getOrderValidationCode());
    }

    @Test
    public void notFromSameRestaurant(){
        testOrder.setPizzasInOrder(new Pizza[]{new Pizza("Margarita", 1000),new Pizza("Meat Lover", 1400 )});
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, testOrder.getOrderValidationCode());
    }


    @Test
    public void invalidNumberOfPizzas(){
        testOrder.setPizzasInOrder(pizzas);
        testOrder.setPriceTotalInPence(7300);
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED,testOrder.getOrderValidationCode());
    }
    @Test
    public void totalAddedCorrectly(){
        testOrder.setPizzasInOrder(new Pizza[]{new Pizza( "Super Cheese", 1400), new Pizza("All Shrooms", 900)});
        testOrder.setPriceTotalInPence(2400);
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.NO_ERROR, testOrder.getOrderValidationCode());
    }

    @Test
    public void totalNotAddedCorrectly(){
        testOrder.setPriceTotalInPence(2700);
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.TOTAL_INCORRECT, testOrder.getOrderValidationCode());
    }

    @Test
    public void restaurantOpen(){
        testOrder.getCreditCardInformation().setCreditCardExpiry("11/26");
        testOrder.setOrderDate(LocalDate.of(2023,11,11));
        testOrder.setPizzasInOrder(new Pizza[] {new Pizza("Margarita", 1000), new Pizza("Calzone", 1400)});
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.NO_ERROR, testOrder.getOrderValidationCode());
    }

    @Test
    public void restaurantClosed(){
        testOrder.setOrderDate(LocalDate.of(2023, 10,11));
        testInstance.validateOrder(testOrder, restaurants);
        assertSame(OrderValidationCode.RESTAURANT_CLOSED, testOrder.getOrderValidationCode());
    }








}
