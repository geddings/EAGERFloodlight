package net.floodlightcontroller.randomizer.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.floodlightcontroller.randomizer.Server;

import java.io.IOException;

/**
 * Created by geddingsbarrineau on 10/29/16.
 */
public class ServerSerializer extends JsonSerializer<Server> {
    @Override
    public void serialize(Server server, JsonGenerator jGen, SerializerProvider sProv)
            throws IOException, JsonProcessingException {
        jGen.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        if (server == null) {
            jGen.writeStartArray();
            jGen.writeString("No EAGER server to report");
            jGen.writeEndArray();
            return;
        }
        jGen.writeStartObject();
        jGen.writeStringField("ip-address-real", server.getiPv4AddressReal().toString());
        jGen.writeStringField("ip-address-fake", server.getiPv4AddressFake().toString());
        jGen.writeStringField("prefix", server.getPrefix().toString());
        jGen.writeEndObject();
    }
}
