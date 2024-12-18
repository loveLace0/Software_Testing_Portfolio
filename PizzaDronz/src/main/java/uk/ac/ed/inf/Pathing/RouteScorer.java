package uk.ac.ed.inf.Pathing;

import uk.ac.ed.inf.Validation.LngLatHandler;
import uk.ac.ed.inf.ilp.data.LngLat;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;

/**
 * Heuristic scorer for A* - looks 3 moves ahead to determine if no-fly zones are ahead and provides a score accordingly
 * in order to avoid them and obtain a smoother route
 */
public class RouteScorer {

    private final LngLatHandler currentCoordinateHandler;

    /**
     * Parameterised default constructor
     * @param handler = takes an instance of the coordinate handler being used during pathfinding
     */
    public RouteScorer(LngLatHandler handler){
        currentCoordinateHandler = handler;
    }

    /**
     * Provides a score on the
     * @param point = current coordinate of routenode being scores
     * @param currentDestination = restaurant currently being routed to
     * @param currentMap = graph used during generation of nodes- contains useful methods
     * @return the score for the routeNode
     */
    public double scoreNeighbour(LngLat point, LngLat currentDestination, Graph currentMap){
        double score = 0;
        boolean noneClose = true;

        //Obtain distanced points from the current point in order to look ahead and order based off relevancy to restaurant
        Set<LngLat> nextPossibleMoves = currentMap.generateImmediateConnected(point,currentDestination,true);
          nextPossibleMoves = nextPossibleMoves.stream().sorted(Comparator.comparingDouble(p -> currentCoordinateHandler.distanceTo((LngLat) p, currentDestination))).collect(toCollection(LinkedHashSet::new));

        //Only examine the 6 most relevant long distance nodes
        if(nextPossibleMoves.size() >6) {
            nextPossibleMoves = new LinkedHashSet<>(nextPossibleMoves.stream().toList().subList(0, 6));
        }

        //For each of the 6, if any are close to any noFlyZones, increase score to deter from that path
        for(LngLat possibleMove: nextPossibleMoves) {
            if (currentMap.closeToAnyRegion(possibleMove)) {
                score += 0.00005;
                noneClose = false;
            }
        }
        //Give small reward to path where none of the future nodes are close- though not too large as to deter path from travelling
        // the most efficient path if it means travelling between no-fly zones
        if(noneClose){
            score -= 0.000051;
        }
        return score;
    }

}
