package uk.ac.ed.inf.Pathing;

/**
 * Class representing each move the drone makes- stores orderNo the move is apart of, the previous coordinates, next coordinates and
 * the angle between the coordinates
 */
public class Move {
    private final String orderNo;
    private final double fromLongitude;
    private final double fromLatitude;
    private final double angle;
    private final double toLongitude;
    private final double toLatitude;

    /**
     * Parameterised constructor to create an instance of the move
     * @param orderNum = number for the order currently being completed that the move is apart of
     * @param fromLong = previous coordinates longitude value
     * @param fromLat = previous coordinates latitude value
     * @param direction = angle between previous and next coordinates
     * @param toLong = next coordinates longitude value
     * @param toLat = next coordinates latitude value
     */
    public Move(String orderNum, double fromLong, double fromLat, double direction, double toLong, double toLat){
        orderNo = orderNum;
        fromLongitude = fromLong;
        fromLatitude = fromLat;
        angle = direction;
        toLongitude = toLong;
        toLatitude = toLat;
    }

    /**
    * The following getters are used during serialisation - each returns an attribute from above
    */
    public String getOrderNo() {
        return orderNo;
    }
    public double getFromLongitude() {
        return fromLongitude;
    }
    public double getFromLatitude() {
        return fromLatitude;
    }
    public double getAngle() {
        return angle;
    }
    public double getToLongitude() {
        return toLongitude;
    }
    public double getToLatitude() {
        return toLatitude;
    }

}
