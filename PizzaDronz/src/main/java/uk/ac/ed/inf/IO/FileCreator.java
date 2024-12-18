package uk.ac.ed.inf.IO;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.geojson.Geometry;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;

import uk.ac.ed.inf.Pathing.Move;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.Order;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for file creation, serialization and logging
 */
public class FileCreator {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String orderDate;
    private String basePath;

    /**
     * Parameterised constructor- will create directory to write desired files to and load in necessary modules for
     * the object mapper
     * @param date = date of orders processed
     */
    public FileCreator(LocalDate date){
        orderDate = date.toString();
        File temp = new File(System.getProperty("user.dir"));
        //Will write to outside target file when ran by jar so it is in main directory of project
        basePath = temp.getParentFile().toString().concat("\\resultfiles");
        File resultsDir = new File(basePath);
        if (!resultsDir.exists()){
            resultsDir.mkdirs();
        }

        //Load custom serializer as a module for the object mapper; will allow mapper to serialise Orders as Deliveries
        SimpleModule module = new SimpleModule("CustomDeliverySerializer", new Version(1, 0, 0, null, null, null));
        module.addSerializer(Order.class, new CustomDeliverySerializer());
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    /**
     * Overloaded method - will serialize orders into deliveries and write to the deliveries file
     * @param deliveries = list of all processed orders to be logged as deliveries
     */
    public void createFile(ArrayList<Order> deliveries){
        //Mapper will serialise array of orders into deliveries in json  and write to the file
        try {
            mapper.writeValue(new File(basePath.concat("\\deliveries-" + orderDate + ".json")), deliveries);

        } catch (IOException e) {
            System.err.println("Failed to write deliveries file");
        }
    }
    /**
     * Overloaded method - will serialize moves and write them to the flightpath file
     * @param moves = list of all moves for each order's flightpath
     */
    public void createFile(List<Move> moves){
        //Mapper serializes array of moves to json and writes to file
        try {
            mapper.writeValue(new File(basePath.concat("\\flightpath-" + orderDate + ".json")), moves);
        } catch (IOException e) {
            System.err.println("Failed to write flightpath file");
        }
    }
    /**
     * Overloaded method - will serialize coordinates and write them to the drone file
     * @param droneMoves = list of coordinates for each flightpath of each order
     * @param routeGenerated =  variable to allow method to be overloaded - way to escape java erasure
     */
    public void createFile(List<LngLat> droneMoves, boolean routeGenerated){
        try {
            //Generate a geoJSON necessary features and add coordinates to LineString object to be serialised
            Feature mainFeature = new Feature();
            Geometry droneRoute = new LineString();
            droneMoves.stream().forEach(p -> droneRoute.add(new double[]{p.lng(),p.lat()}));
            mainFeature.setGeometry(droneRoute);

            //Mapper serializes FeatureCollection and writes to file
            mapper.writeValue(new File(basePath.concat("\\drone-" + orderDate + ".geojson")), new FeatureCollection().add(mainFeature));

        } catch (IOException e) {
            System.err.println("Failed to write drone file");
        }
    }
}
