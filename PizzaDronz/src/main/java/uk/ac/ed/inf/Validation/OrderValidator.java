package uk.ac.ed.inf.Validation;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;


public class OrderValidator implements uk.ac.ed.inf.ilp.interfaces.OrderValidation {

    /**
     * Validates the given order with respect to the orders' payment details, the pizzas and the restaurant
     * @param orderToValidate = the current order to be validated
     * @param definedRestaurants = list of restaurants to compare order restaurants to
     * @return order with a set OrderValidation code
     */
    @Override
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {
        CreditCardInformation cardToValidate = orderToValidate.getCreditCardInformation();

        //Validating CVV with regex to ensure only 3 numbers accepted
        if(!cardToValidate.getCvv().matches("^[0-9]{3}$")){
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
        }

        //Checking credit card number contains 16 valid numbers
        if(!cardToValidate.getCreditCardNumber().matches("^[0-9]{16}$")){
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
        }

        try {
            //Parse to check expiry date is in correct format, any format exception thrown caught
            SimpleDateFormat format = new SimpleDateFormat("MM/yy");
            format.parse(orderToValidate.getCreditCardInformation().getCreditCardExpiry());

            //Extract month and year from string and ensure year and month captured correctly
            LocalDate expiry = LocalDate.of(Integer.parseInt(String.valueOf(orderToValidate.getOrderDate().getYear()).substring(0,2) + cardToValidate.getCreditCardExpiry().substring(3,5)), Integer.parseInt(cardToValidate.getCreditCardExpiry().substring(0,2)), 5);

            //Set day to last day of month to ensure card valid for full expiry date month
            expiry = LocalDate.of(expiry.getYear(), expiry.getMonth(), expiry.getMonth().maxLength());

            //Check if order is past expiry date ie invalid card
            if(expiry.isBefore(orderToValidate.getOrderDate())){
                throw new Exception();
            }
        } catch (Exception e) {
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
        }
        //Variable to establish total cost of pizzas
        int totalOrderCost = 0;

        //Flag for determining if the current pizza has been found within any of the restaurants menus
        boolean pizzaExists = false;

        //Variables to store the found restaurants opening hours (for later validation) and name
        DayOfWeek[] openingHours = null;
        String restaurant = null;

        pizzaLoop:
        for(int pizzaNumber = 0; pizzaNumber < orderToValidate.getPizzasInOrder().length; pizzaNumber++){
            //reset flag with each pizza iteration to ensure each pizza exists within defined restaurants
            pizzaExists = false;
            restaurantLoop:
            for(int currentRestaurant = 0; currentRestaurant < definedRestaurants.length; currentRestaurant++){
                for(int menuItem = 0; menuItem < definedRestaurants[currentRestaurant].menu().length; menuItem++){
                    if(definedRestaurants[currentRestaurant].menu()[menuItem].name().equals(orderToValidate.getPizzasInOrder()[pizzaNumber].name())){
                        //Current pizza exists hence set flag to true
                        pizzaExists = true;

                        //If current restaurant has not been found then assign it, else if current pizzas restaurant doesnt match previously assigned it must be
                        //from a different restaurant hence set validation code and break
                        if(restaurant == null){
                            restaurant = definedRestaurants[currentRestaurant].name();
                        }
                        else if(!restaurant.equals(definedRestaurants[currentRestaurant].name())){
                            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
                            break pizzaLoop;
                        }
                        openingHours = definedRestaurants[currentRestaurant].openingDays();

                        //Break restaurant loop to get to next pizza as current pizza has been found
                        break restaurantLoop;
                    }
                }
            }
            //If flag has not been set then pizza must not be within defined set of restaurants
            if(!pizzaExists){
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                break;
            }
            totalOrderCost += orderToValidate.getPizzasInOrder()[pizzaNumber].priceInPence();
        }

        //If not one of the days in opening hours of current restaurant matches the order date day then restaurant must be closed
        if(openingHours != null && Arrays.stream(openingHours).filter(d->d.equals(orderToValidate.getOrderDate().getDayOfWeek())).toList().isEmpty()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
        }

        //Cost calculated so far plus the delivery charge must equal the orders price else the total must be incorrect
        if(totalOrderCost + SystemConstants.ORDER_CHARGE_IN_PENCE != orderToValidate.getPriceTotalInPence() && orderToValidate.getOrderValidationCode() == OrderValidationCode.UNDEFINED){
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
        }

        //Ensure no more than 4 pizzas have been ordered
        if(orderToValidate.getPizzasInOrder().length > SystemConstants.MAX_PIZZAS_PER_ORDER){
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
        }

        //Check no other validation codes have been set and if not then order is error free
        if(orderToValidate.getOrderValidationCode() == OrderValidationCode.UNDEFINED){
            orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
            orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        }
        else{
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
        }
        return orderToValidate;
    }
}
