package uk.ac.ed.inf;

import uk.ac.ed.inf.IO.FileCreator;
import uk.ac.ed.inf.IO.RestInfoRetriever;
import uk.ac.ed.inf.Pathing.FlightPath;
import uk.ac.ed.inf.Pathing.Move;
import uk.ac.ed.inf.Pathing.PathFinder;
import uk.ac.ed.inf.Validation.InputHandler;
import uk.ac.ed.inf.Validation.OrderValidator;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.*;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Main class - startpoint of application
 *
 *
 * ross points
 * - possible easier way to make files and directories!
 *
 * - maybe pull URL into a function?
 * - line 107 for loop can maybe be done in a stream
 * - 3 different file creation methods
 * - account for dates w/ no orders
 * - do central area checks
 * - make sure to just return null in the case that an order has no valid path instead of throwing an exception
 */
public class App 
{
    private static ArrayList<Order> orders;
    private static ArrayList<Restaurant> restaurants;
    private static ArrayList<NamedRegion> noZones;
    private static NamedRegion central;
    private static ArrayList<LngLat> route = new ArrayList<>();
    private static ArrayList<Move> allMoves = new ArrayList<>();

    /**
     * Main start point in application- will call classes from across the application to validate commandline arguments,
     * process orders, route to restaurants and write results to files
     * @param args = commandline arguments
     */
    public static void main(String[] args )
    {
        System.out.println("Welcome to PizzaDronz flightpathing and logging system");

        //Validate commandline arguments and get order date and URL
        InputHandler inputs = new InputHandler(args);
        LocalDate orderDate = inputs.getOrderDate();
        URL url = inputs.getWebsite();
        final DayOfWeek day = orderDate.getDayOfWeek();

        // Create RestInformationRetriever object to obtain  data from the rest service
        RestInfoRetriever retriever = new RestInfoRetriever();
        retriever.connect(url);

        //Get data from respective endpoints
        try{
            orders = (ArrayList<Order>) retriever.jsonReader(new URL(url+ "/orders/" + orderDate), "Order");
            restaurants = ((ArrayList<Restaurant>)retriever.jsonReader(new URL(url + "/restaurants"), "Restaurant"));
            noZones = ((ArrayList<NamedRegion>)retriever.jsonReader(new URL(url + "/noFlyZones"), "NamedRegion"));
            central = ((ArrayList<NamedRegion>) retriever.jsonReader(new URL(url + "/centralArea"), "Central")).get(0);
        }
        catch(Exception e){
            errorMessage("Incorrect URL for rest end point");
        }
        System.out.println("Connected to rest server");

        //Instantiate file creator with given order date
        FileCreator fileGenerator = new FileCreator(orderDate);

        //Filter restaurants to only consider restaurants that are open that day
        Restaurant[] filteredRestaurants = Arrays
                .stream(restaurants.toArray(new Restaurant[restaurants.size()]))
                .filter(r-> Arrays.stream(r.openingDays()).toList().contains(day))
                .toArray(size -> new Restaurant[size]);

        //Configure flightRouter with only valid restaurants and NamedRegions
        PathFinder flightRouter = new PathFinder(filteredRestaurants, noZones.toArray(new NamedRegion[noZones.size()]), central);

        //Generate route and store path for each restaurant
        System.out.println("Generating routes...");
        flightRouter.createAllRoutes();
        System.out.println("Routes generated");

        OrderValidator validator = new OrderValidator();
        HashMap<Pizza, Restaurant> restaurantsAndItems = new HashMap<>();

        //Create map of all unique pizzas and their accompanying restaurant - used when obtaining restaurant for an order
        for(Restaurant restaurant: filteredRestaurants){
            List<Pizza> pizzas = new ArrayList(Arrays.stream(restaurant.menu()).toList());
            pizzas.stream().forEach(p -> restaurantsAndItems.put(p, restaurant));
        }

        for(Order currentOrder : orders){
            currentOrder = validator.validateOrder(currentOrder,filteredRestaurants);
            if(currentOrder.getOrderStatus() == OrderStatus.VALID_BUT_NOT_DELIVERED){
                List<LngLat> rawRoute = flightRouter.getRoute(restaurantsAndItems.get(currentOrder.getPizzasInOrder()[0]));
                //If route empty then no route was found for that restaurant, hence order wont be delivered even if valid
                if(!rawRoute.isEmpty()) {
                    route.addAll(rawRoute);
                    allMoves.addAll(new FlightPath(currentOrder.getOrderNo(), rawRoute, flightRouter.getGraph()).getProcessedRoute());
                    currentOrder.setOrderStatus(OrderStatus.DELIVERED);
                }
            }
        }
        System.out.println("Generating results...");
        //Create and populate result files for Deliveries, Flightpath and Drone respectively
        fileGenerator.createFile(orders);
        fileGenerator.createFile(route,true);
        fileGenerator.createFile(allMoves);

        System.out.println("----------------------------------\n" + "Orders processed successfully and routes generated: see results file for paths and processed orders");
    }

    public static void errorMessage(String message){
        System.err.println(message + ". Program terminating...");
        System.exit(1);
    }
}
