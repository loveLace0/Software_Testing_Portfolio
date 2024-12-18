package uk.ac.ed.inf;
import org.junit.Test;
import uk.ac.ed.inf.Validation.LngLatHandler;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import static org.junit.Assert.*;

public class LngLatHandlingTests {

    LngLatHandler testInstance = new LngLatHandler();
    LngLat forestHill = new LngLat(-3.192473,55.946233);
    LngLat buccleuch = new LngLat(-3.184319,55.942617);
    LngLat kfc = new LngLat(-3.184319,55.946233);
    LngLat meadows = new LngLat(-3.192473,55.942617);

    LngLat[] region = {forestHill, meadows, buccleuch, kfc};
    LngLat[] multisideRegion = {new LngLat(3.10,55.90), new LngLat(3.11, 55.94), new LngLat(3.15, 55.89), new LngLat(3.23, 55.94), new LngLat(3.25,55.95)};
    NamedRegion central = new NamedRegion("Central Area", region);
    NamedRegion pentagon = new NamedRegion("Pentagon", multisideRegion);

    @Test
    public void correctDistanceCalculated(){
        double result = testInstance.distanceTo(forestHill, buccleuch);
        double expected = Math.sqrt((Math.pow(-3.192473 - -3.184319, 2))+(Math.pow(55.946233-55.942617, 2)));
        assertEquals(result,expected,0.00);
    }

    @Test
    public void closeToPoint(){
        LngLat closePoint = new LngLat(-3.192472, 55.946232);
        assertTrue(testInstance.isCloseTo(forestHill, closePoint));
    }

    @Test
    public void notCloseToPoint(){
        LngLat notClosePoint = new LngLat(-3.92472, 55.46232);
        assertFalse(testInstance.isCloseTo(forestHill, notClosePoint));
    }

    @Test
    public void closeToBeingClose(){
        LngLat nearlyClose = new LngLat(-3.199999, 56.125465);
        assertFalse(testInstance.isCloseTo(forestHill, nearlyClose));
    }


    @Test
    public void withinRegion(){
        LngLat withinRegion = new LngLat(-3.192163,55.945233);
        assertTrue(testInstance.isInRegion(withinRegion, central));
    }

    @Test
    public void notWithinRegion(){
        LngLat notWithinRegion = new LngLat(-3.261934, 55.943617);
        assertFalse(testInstance.isInRegion(notWithinRegion, central));
    }

    @Test
    public void withinMultiPointRegion(){
        LngLat withinRegion = new LngLat(3.17, 55.91);
        assertTrue(testInstance.isInRegion(withinRegion, pentagon));
    }

    @Test
    public void notWithinMultiPointRegion(){
        LngLat notWithin = new LngLat(3.105, 55.895);
        assertFalse(testInstance.isInRegion(notWithin, pentagon));
    }


    @Test
    public void nextPointCorrect_E(){
        LngLat oldPoint = new LngLat(3.11111,52.222222);
        LngLat newPoint = new LngLat(3.11126, 52.222222);
        assertEquals(testInstance.nextPosition(oldPoint, 0.00), newPoint);
    }

    @Test
    public void nextPointCorrect_N(){
        LngLat oldPoint = new LngLat(3.11111,52.222222);
        LngLat newPoint = new LngLat(3.11111, 52.222372);
        assertEquals(testInstance.nextPosition(oldPoint, 90.00), newPoint);
    }

}
