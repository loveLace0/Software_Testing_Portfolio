package uk.ac.ed.inf.Validation;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;
import java.awt.geom.Path2D;

public class LngLatHandler implements LngLatHandling {

    /**
     * Find the distance between the start and end positions using pythagorean formula
     * @param startPosition = initial LatLng coordinate of drone
     * @param endPosition = destination coordinate of drone
     * @return distance between points
     */
    @Override
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        return Math.sqrt(Math.pow(startPosition.lng()-endPosition.lng(), 2)+Math.pow(startPosition.lat()-endPosition.lat(), 2));
    }

    /**
     * Returns result of if two coordinates are close to eachother ie within 0.00015 distance of eachother
     * @param startPosition = first coordinate to consider
     * @param otherPosition = coordinate that is compared to for closeness
     * @return boolean value indicating if the points are close
     */

    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) < SystemConstants.DRONE_IS_CLOSE_DISTANCE;
    }

    /**
     * Determines if a given point is within a specified region
     * @param position = coordinate to establish if it is within the given zone or not
     * @param region = zone that coordinate is being compared to
     * @return boolean value to indicate if position is within designated boundary or not
     */

    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        final Path2D boundary = new Path2D.Double();
        boundary.moveTo(region.vertices()[0].lng(),region.vertices()[0].lat());

        //Create perimeter of region
        for(int i = 1; i < region.vertices().length; i++) {
            boundary.lineTo(region.vertices()[i].lng(), region.vertices()[i].lat());
        }
        if(region.vertices()[region.vertices().length-1] != region.vertices()[0]){
            boundary.closePath();
        }

        //Determine if point is within region
        return boundary.contains(position.lng(), position.lat());
    }

    /**
     * Finds the drones next position when given its current coordinate and the desired direction
     * @param startPosition = drones initial coordinate
     * @param angle = direction desired given in degrees
     * @return new coordinate in given direction 0.00015 away
     */
    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        return new LngLat((startPosition.lng() + SystemConstants.DRONE_MOVE_DISTANCE*Math.cos(Math.toRadians(angle))), (startPosition.lat() + SystemConstants.DRONE_MOVE_DISTANCE*Math.sin(Math.toRadians(angle))));
    }

    /**
     * Finds the next position 3 moves away in a given direction from a set point - used for heuristic
     * @param startPosition = drones initial coordinate
     * @param angle = direction desired given in degrees
     * @return new coordinate in given direction 0.00045 away
     */
    public LngLat nextFarPosition(LngLat startPosition, double angle ) {
        return new LngLat((startPosition.lng() + 0.00045*Math.cos(Math.toRadians(angle))), (startPosition.lat() + 0.00045*Math.sin(Math.toRadians(angle))));
    }

}
