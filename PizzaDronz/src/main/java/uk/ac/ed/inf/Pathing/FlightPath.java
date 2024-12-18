package uk.ac.ed.inf.Pathing;

import uk.ac.ed.inf.ilp.data.LngLat;
import java.util.ArrayList;
import java.util.List;
/**
 * Class for converting raw coordinate route into processed route consisting of drone moves
 */
public class FlightPath {
    private static String orderNo;
    private static List<LngLat> unprocessedRoute;
    private static ArrayList<Move> processedRoute;
    private final Graph graph;

    /**
     * Parameterised constructor - takes raw route of LngLat points and processes it into a series of moves for flightpath
     * @param orderNum = Order identifier
     * @param rawRoute = List of LngLat coordinates to be processed to moves
     * @param map = Graph used to generate the raw route; contains information on the connections and corresponding angles
     */
    public FlightPath(String orderNum, List<LngLat> rawRoute, Graph map){
        orderNo = orderNum;
        unprocessedRoute = rawRoute;
        graph = map;
        processedRoute = new ArrayList<>(rawRoute.size()+2);
        parseRoute();
    }

    /**
     * Processes each LngLat into a Move and stores result
     */
    private void parseRoute(){
        //For each Lnglat point in the raw route, create a new move using it and its next coordinate
        int i;
        for(i = 0; i < unprocessedRoute.size()-1; i++){
            processedRoute.add(new Move(orderNo, unprocessedRoute.get(i).lng(),
                    unprocessedRoute.get(i).lat(),
                    graph.getCorrespondingAngle(unprocessedRoute.get(i), unprocessedRoute.get(i+1)),
                    unprocessedRoute.get(i+1).lng(),
                    unprocessedRoute.get(i+1).lat()));
        }
        //Ensure drone hovers when it has arrived at the restaurant
        processedRoute.add(new Move(orderNo, unprocessedRoute.get(i).lng(), unprocessedRoute.get(i).lat(), 999.0, unprocessedRoute.get(i).lng(), unprocessedRoute.get(i).lat()));
    }

    public ArrayList<Move> getProcessedRoute() {
        return processedRoute;
    }
}
