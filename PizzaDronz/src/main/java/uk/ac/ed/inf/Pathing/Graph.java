package uk.ac.ed.inf.Pathing;

import uk.ac.ed.inf.Validation.LngLatHandler;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Restaurant;
import java.util.*;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toCollection;

/**
 * Class for representing the map and producing necessary valid nodes for pathfinding
 */
public class Graph{
    //Map containing all connections for each point, all connections stored with their corresponding angle
    private final Map<LngLat,HashMap<LngLat,Double>> connections= new HashMap<>();
    private final NamedRegion[] noZones;
    private final LngLatHandler handler = new LngLatHandler();
    private final NamedRegion central;

    //List of all points on the map including no-fly zone points and central - used for boundary creation to limit search
    private static ArrayList<LngLat> allPoints = new ArrayList<>();

    /**
     * Parameterised constructor for establishing no fly zones and obtaining restaurants
     * @param regions list of no fly zones
     * @param startPoint initial point drone will start from (appleton for now however left as a variable incase this changes
     * @param restaurants list of restauants to have routes generated to
     * @param centralRegion the defined central region area
     */
    public Graph(NamedRegion[]regions, LngLat startPoint, Restaurant[] restaurants, NamedRegion centralRegion){
        noZones = regions;
        central = centralRegion;
        Set<LngLat> destinations = Arrays.stream(restaurants).map(Restaurant::location).collect(Collectors.toSet());

        //Obtain coordinates from all points from regions entered and restaurants to create boundary for search
        for (NamedRegion region : regions) {
            Collections.addAll(allPoints, Arrays.stream(region.vertices()).distinct().toList().toArray(new LngLat[region.vertices().length-1]));
        }
        allPoints.add(startPoint);
        allPoints.addAll(destinations);
        Collections.addAll(allPoints, Arrays.stream(centralRegion.vertices()).distinct().toList().toArray(new LngLat[4]));
    }
    /**
     * Generate 16 coordinates a set distance from the start point and order them based off their distance to the
     * destination being currently routed
     * @param start = point that will have the 16 points generated from
     * @param currentDestination = destination being routed to - used in relation to ordering the 16 points
     * @param lookahead = flag for if method is being used by the graph or by the heuristic in A*:
     *                    if true: will calculate points 0.00045 away from the start point
     *                    if false: will calculate the immediately connected points 0.00015 away
     * @return the 6 closest points to the destination - helps narrow the search for A*
     */
    public Set<LngLat> generateImmediateConnected(LngLat start, LngLat currentDestination, boolean lookahead){
        //Set for nodes used in search only
        Set<LngLat> connectedNodesForSearch= new HashSet<>();
        HashMap<LngLat,Double> connected = new HashMap<LngLat, Double>();

        //For all 16 possible directions the drone can move
        for(Double angle = 0.0; angle < 360; angle+= 22.5){
            LngLat nextConnection;
            //If being called by the simpleLineScorer for calculating the heuristic, use the handlers nextFarPosition
            if(lookahead){
                nextConnection = handler.nextFarPosition(start,angle);
            }
            else {
                nextConnection = handler.nextPosition(start, angle);
                connected.put(nextConnection,angle);
            }
            //If generated node isn't within any no-fly zone or isn't out of bounds then add
            if(notInAnyRegions(nextConnection) && !outOfBounds(nextConnection)){
                connectedNodesForSearch.add(nextConnection);
                connected.put(nextConnection,angle);
            }
        }
        //Only add to official connections list if it's an immediate connection ie 0.00015 away
        if(!lookahead) {
            connections.put(start, connected);
        }

        //Sort the nodes based off distance to the final destination and only return the closest 8
        connectedNodesForSearch = connectedNodesForSearch.stream().sorted(Comparator.comparingDouble(p -> handler.distanceTo(p, currentDestination))).collect(toCollection(LinkedHashSet::new));
        if(connectedNodesForSearch.size() > 6){
            connectedNodesForSearch = new LinkedHashSet<>(connectedNodesForSearch.stream().toList().subList(0, 6));
        }
        return connectedNodesForSearch;
    }

    /**
     * Determines if a given coord is within any of the provided noFlyZones
     * @param coord = point to be checked
     * @return result of if point has been found within any noFlyZones
     */
    private boolean notInAnyRegions(LngLat coord){
        boolean notInRegions = true;
        for (NamedRegion noZone : noZones) {
            if (handler.isInRegion(coord, noZone)) {
                notInRegions = false;
                break;
            }
        }
        return notInRegions;
    }

    public NamedRegion getCentral(){
        return central;
    }

    /**
     * Determines if a point is close to any given region - used for A* heuristic
     * @param coord = point to be checked if it close to any no-fly zones
     * @return boolean value corresponding to if the point is within 0.00015 of any no fly regions
     */
    public boolean closeToAnyRegion(LngLat coord){
        boolean closeTo = false;
        for(NamedRegion noZone:noZones){
            //Obtain points of noFlyZones and sort them based off distance to point to be checked
            LngLat[] points = noZone.vertices();
            points = Arrays.stream(points).sorted(Comparator.comparingDouble(v -> handler.distanceTo(coord, v))).toList().toArray(new LngLat[points.length]);

            //If the point to be checked is within 0.00015 of the closest point to it then it is close to the region
            if (handler.isCloseTo(coord, points[0])) {
                closeTo = true;
                break;
            }
        }
        return closeTo;
    }
    /**
     * Creates a boundary using the extreme points from all points within graph and determines
     * if a given point is within that boundary - aids the search by keeping the nodes relevant to their destination
     * @param coord = point to be cross referenced for if its within the boundary
     * @return result of if point is outOfBounds or not
     */
    private boolean outOfBounds(LngLat coord){
        //Obtain extreme points from the collection of all points
        LngLat maxLat = Collections.max(allPoints, Comparator.comparing(LngLat::lat));
        LngLat minLat = Collections.min(allPoints, Comparator.comparing(LngLat::lat));
        LngLat maxLng = Collections.max(allPoints, Comparator.comparing(LngLat::lng));
        LngLat minLng = Collections.min(allPoints, Comparator.comparing(LngLat::lng));

        //Take extremes and increase them by one move to allow for case where drone would need to move out around extremes if blocked
        LngLat bottomLeft = new LngLat(minLng.lng()- SystemConstants.DRONE_MOVE_DISTANCE, minLat.lat()-SystemConstants.DRONE_MOVE_DISTANCE);
        LngLat topLeft = new LngLat(minLng.lng()-SystemConstants.DRONE_MOVE_DISTANCE, maxLat.lat()+SystemConstants.DRONE_MOVE_DISTANCE);
        LngLat bottomRight = new LngLat(maxLng.lng()+SystemConstants.DRONE_MOVE_DISTANCE, minLat.lat()-SystemConstants.DRONE_MOVE_DISTANCE);
        LngLat topRight = new LngLat(maxLng.lng()+SystemConstants.DRONE_MOVE_DISTANCE, maxLat.lat()+SystemConstants.DRONE_MOVE_DISTANCE);

        //If the point is not within this region then wil return false
        return !handler.isInRegion(coord,new NamedRegion("Total Area",new LngLat[]{bottomLeft, topLeft, topRight, bottomRight}));
    }

    /**
     * Returns corresponding angle between a point and one of its immediate connections
     * @param parentNode = point that connectingNode was produced from
     * @param connectingNode = node produced from parent node in relation to the requested angle
     * @return angle between parentNode and connectingNode
     */
    public Double getCorrespondingAngle(LngLat parentNode, LngLat connectingNode){
        Double angle;
        //Check for a hover move first
        if(parentNode == connectingNode){
            angle = 999.0;
            return angle;
        }
        //Connections will contain parents and connectingNodes for the route *TO* the restaurant, not return, hence
        //check if connections contains the parent first, if it doesnt then it's for the return route so swap parent and connecting
        if(connections.containsKey(parentNode)){
            angle = connections.get(parentNode).get(connectingNode);
        }
        else{
            angle = connections.get(connectingNode).get(parentNode);
            if (angle < 180) {
                angle += 180;
            }
            else {
                angle -= 180;
            }
        }
        return angle;
    }
}
