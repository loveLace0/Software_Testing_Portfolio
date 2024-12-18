package uk.ac.ed.inf.IO;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import uk.ac.ed.inf.ilp.data.Order;

import java.io.IOException;

/**
 * Class for serialising orders in the form of deliveries
 */
public class CustomDeliverySerializer extends StdSerializer<Order> {
    public CustomDeliverySerializer() {
        this(null);
    }

    public CustomDeliverySerializer(Class<Order> t) {
        super(t);
    }

    /**
     * Serializes Order as delivery information
     * @param order = Order to be serialised
     * @param jsonGenerator = Jackson jsonGenerator object for writing values to json
     * @param serializer = Jackson serializer- called during serialization in FileCreator
     */
    @Override
    public void serialize(Order order, JsonGenerator jsonGenerator, SerializerProvider serializer) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("orderNo", order.getOrderNo());
        jsonGenerator.writeStringField("orderStatus", order.getOrderStatus().toString());
        jsonGenerator.writeStringField("orderValidationCode", order.getOrderValidationCode().toString());
        jsonGenerator.writeNumberField("costInPence", order.getPriceTotalInPence());
        jsonGenerator.writeEndObject();
    }

}
