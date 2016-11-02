package net.floodlightcontroller.randomizer.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.floodlightcontroller.randomizer.Connection;

import java.io.IOException;

/**
 * Created by geddingsbarrineau on 11/2/16.
 *
 */
public class ConnectionSerializer extends JsonSerializer<Connection> {
    @Override
    public void serialize(Connection connection, JsonGenerator jGen, SerializerProvider sProv)
            throws IOException, JsonProcessingException {
        jGen.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        if (connection == null) {
            jGen.writeStartArray();
            jGen.writeString("No EAGER connections to report");
            jGen.writeEndArray();
            return;
        }
        jGen.writeStartObject();
        jGen.writeObjectField("server", connection.getServer());
//        jGen.writeStringField("ip-address-fake", server.getiPv4AddressFake().toString());
//        jGen.writeStringField("prefix", server.getPrefix().toString());
        jGen.writeEndObject();
    }
}