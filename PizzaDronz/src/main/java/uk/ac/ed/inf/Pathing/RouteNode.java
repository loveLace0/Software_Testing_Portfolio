package uk.ac.ed.inf.Pathing;

import uk.ac.ed.inf.ilp.data.LngLat;
/**
 * Class creates nodes for representation on the map during pathfinding. Stores their scores in relation to how good
 * they are for an efficient path and the previous node to itself in the route
 */
public class RouteNode implements Comparable<RouteNode> {
    private final LngLat current;
    private LngLat previous;
    private double routeScore;
    private double estimatedScore;

    /**
     * Constructor for creating a new Node, will set routescore and estimated score as least desired ie
     * very large for heuristic
     * @param node = point to be used for node creation
     */
    public RouteNode(LngLat node){
        current = node;
        previous = null;
        routeScore = Double.POSITIVE_INFINITY;
        estimatedScore = Double.POSITIVE_INFINITY;
    }

    /**
     * Parameterised constructor for node creation. Will allow for assigning actual and estimated scores
     * @param node = point to be used for node creation
     * @param previousNode = point from which node is a connection, the previous point in the path
     * @param scoreReal = actual score - used for determining which node in a set of connections to set as previous node
     * @param scoreEstimated = estimated score - takes into account total distance so far as well as heuristics score,
     *                       used for determining which node is taken from the frontier to expand
     */
    public RouteNode(LngLat node, LngLat previousNode, double scoreReal, double scoreEstimated){
        current = node;
        previous = previousNode;
        routeScore = scoreReal;
        estimatedScore = scoreEstimated;
    }

    public LngLat getPrevious() {
        return previous;
    }
    public double getRouteScore(){
        return routeScore;
    }
    public LngLat getCurrent(){
        return current;
    }

    /**
     * Overriden compareTo - used in priority queue for ordering routenodes by estimated score
     * @return value indicating how the current routenodes estimated score compares to any routenode being compared to
     */
    @Override
    public int compareTo(RouteNode other) {
        if(this.estimatedScore > other.estimatedScore){
            return 1;
        }
        else if(this.estimatedScore < other.estimatedScore){
            return -1;
        }
        else {
            return 0;
        }
    }

    public void setPrevious(LngLat node) {
        previous = node;
    }
    public void setRouteScore(double newRouteScore) {
        routeScore = newRouteScore;
    }
    public void setEstimatedScore(double newEstimateScore) {
        estimatedScore = newEstimateScore;
    }
}
