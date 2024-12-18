package uk.ac.ed.inf.Pathing;

import uk.ac.ed.inf.Validation.LngLatHandler;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Class for finding the most efficient path to each restaurant desired
 */
public class PathFinder {
    private final Restaurant[] restaurantsToPath;
    private final LngLat appletonTower = new LngLat(-3.186874, 55.944494);
    private final Graph graph;
    private final HashMap<String,List<LngLat>> restaurantRoutes = new HashMap<>();
    private final LngLatHandler distanceScorer = new LngLatHandler();
    private final RouteScorer routeScorer = new RouteScorer(distanceScorer);
    private final LngLatHandler coordinateHandler = new LngLatHandler();

    /**
     * Parameterised constructor for setting variables and creating a new graph to be used for pathing
     * @param filteredRestaurants = list of restaurants open on date of order
     * @param noFlyZones = list of noFlyZones obtained from the rest server
     * @param central = the central area region
     */
    public PathFinder(Restaurant[] filteredRestaurants, NamedRegion[] noFlyZones, NamedRegion central){
        restaurantsToPath=filteredRestaurants;
        graph = new Graph(noFlyZones, appletonTower, restaurantsToPath, central);
    }
    /**
     * Finds route for each restaurant and stores result within a hashmap
     */
    public void createAllRoutes(){
        //For each restaurant, call local method to generate route to the restaurant from Appleton
        List<LngLat>route = new ArrayList<>();
        for(Restaurant restaurant:restaurantsToPath){
            try {
                route = findRouteToRestaurant(restaurant);
                //Generate return route and add to total route
                List<LngLat> returnRoute = new ArrayList<>(route);
                Collections.reverse(returnRoute);
                route.addAll(returnRoute);
            }
            //If no route is found for that restaurant
            catch(IllegalStateException e){
                System.err.println("No route found for " + restaurant.name() + ", valid orders will not be delivered");
            }
            //Will continue routing for other restaurants but inform user of no route being found
            restaurantRoutes.put(restaurant.name(), route);
        }
    }
    /**
     * Returns route of coordinates
     * @param location = the restaurant for which the route is to be returned
     * @return the raw route consisting of a list of coordinates
     */
    public List<LngLat> getRoute(Restaurant location){
        return restaurantRoutes.get(location.name());
    }

    /**
     * Returns the graph used during the pathing process
     * @return graph object containing relevant points and connections
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Finds most efficient path to the restaurant. Implements  A* with a simple  heuristic for avoiding future
     * no-fly zones
     * @param currentRestaurant = restaurant to determine route to
     * @return the generated route to the restaurant
     */
    public List<LngLat> findRouteToRestaurant(Restaurant currentRestaurant) {
        Queue<RouteNode> openSet = new PriorityQueue<>();
        Map<LngLat,RouteNode> allNodes = new HashMap<>();
        RouteNode start = new RouteNode(appletonTower, null, 0d,distanceScorer.distanceTo(appletonTower,currentRestaurant.location()));
        List<LngLat> route = new ArrayList<>();
        AtomicBoolean leftCentral = new AtomicBoolean(false);

        openSet.add(start);
        allNodes.put(appletonTower, start);

        //While frontier isn't empty take the top route node from the set
        while (!openSet.isEmpty()) {
            RouteNode current = openSet.poll();
            //If routenode is close to the destination then iterate back through all nodes and obtain routenodes in the path
            if(distanceScorer.isCloseTo(current.getCurrent(),currentRestaurant.location())){
                RouteNode node = current;
                do {
                    route.add(0, node.getCurrent());
                    node = allNodes.get(node.getPrevious());
                } while (node != null);
                return route;
            }
            //otherwise, generate the immediately connected coordinates for that route node
            try{
                graph.generateImmediateConnected(current.getCurrent(),currentRestaurant.location(),false).forEach(connection-> {
                    if(!leftCentral.get()){
                        //If the node has a previous node set to it, check if the previous was in central and the current is not ie has it left
                        if(current.getPrevious()!=null){
                            leftCentral.set(coordinateHandler.isInRegion(current.getPrevious(), graph.getCentral()) &&
                                !coordinateHandler.isInRegion(current.getCurrent(), graph.getCentral()));
                        }
                    }
                    RouteNode nextNode = allNodes.getOrDefault(connection, new RouteNode(connection));
                    //If the drone has left, and the current connection is within central ie re-entering central, skip this connection
                    if(leftCentral.get()){
                        if(coordinateHandler.isInRegion(nextNode.getCurrent(),graph.getCentral())){
                            return;
                        }
                    }
                    //Score each connection based off difference in distance to the goal, distance travelled so far and score in relation to
                    // future no fly zones being in the way
                    double newScore = current.getRouteScore()  + (distanceScorer.distanceTo(current.getCurrent(),currentRestaurant.location()) - distanceScorer.distanceTo(nextNode.getCurrent(),currentRestaurant.location())) +
                            routeScorer.scoreNeighbour(nextNode.getCurrent(), currentRestaurant.location(),graph);
                    allNodes.put(connection, nextNode);

                    //If the new score is better ie less than the route nodes score then set the current routescore as
                    //the new score to shorten the search
                    if (newScore < nextNode.getRouteScore()) {
                        current.setRouteScore(newScore);
                        nextNode.setPrevious(current.getCurrent());
                        nextNode.setRouteScore(newScore);

                        //Set estimated score as actual distance of next node to the destination + heuristic score
                        nextNode.setEstimatedScore(newScore+ distanceScorer.distanceTo(nextNode.getCurrent(), currentRestaurant.location()) + routeScorer.scoreNeighbour(nextNode.getCurrent(),currentRestaurant.location(),graph));
                        openSet.add(nextNode);
                    }
                });
            }
            catch (NullPointerException e){
                 System.out.println("Failed generate more points ");
            }
        }
        System.err.println("No viable route found");
        throw new IllegalStateException();
    }
}
