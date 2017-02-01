package net.floodlightcontroller.randomizer.web;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import net.floodlightcontroller.randomizer.IRandomizerService;
import org.projectfloodlight.openflow.types.IPAddressWithMask;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Created by geddingsbarrineau on 2/1/17.
 */
public class PrefixResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(PrefixResource.class);
    protected static final String STR_CURRENT = "current";
    protected static final String STR_ALL = "all";
    protected static final String STR_OPERATION_ADD = "add";
    protected static final String STR_OPERATION_REMOVE = "remove";
    
    /* TODO: Add more error checking here */
    
    @Get
    public Object getPrefixes() {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());
        String scope = (String) getRequestAttributes().get("scope");
        
        if (getRequestAttributes().containsKey("operation")) {
            return Collections.singletonMap("ERROR", "Prefix operation must be in either a PUT or POST message");
        }
        
        if (scope.equals(STR_CURRENT)) {
            return Collections.singletonMap("current-prefix", randomizerService.getCurrentPrefix().toString());
        }
        
        if (scope.equals(STR_ALL)) {
            
            return Collections.singletonMap("all-prefixes", randomizerService.getPrefixes().stream()
                    .map(IPAddressWithMask::toString)
                    .collect(Collectors.toList()));
                    
        }
        
        return Collections.singletonMap("ERROR", "Unimplemented configuration option");
    }
    
    @Put
    @Post
    public Object addPrefixes(String json) {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());
        String operation = (String) getRequestAttributes().get("operation");

        if (getRequestAttributes().containsKey("scope")) {
            return Collections.singletonMap("ERROR", "Prefix scope must only be in a GET message");
        }
        
        if (operation.equals(STR_OPERATION_ADD)) {
            randomizerService.addPrefix(parsePrefixFromJson(json));
            return Collections.singletonMap("SUCCESS", "Prefix added!");
        }
        
        if (operation.equals(STR_OPERATION_REMOVE)) {
            randomizerService.removePrefix(parsePrefixFromJson(json));
            return Collections.singletonMap("SUCCESS", "Prefix removed!");
        }

        return Collections.singletonMap("ERROR", "Unimplemented configuration option");
    }

    /**
     * Expect JSON:
     * {
     * 		"ip-address"	:	"valid-ip-address",
     * 	    "mask"          :   "valid-ip-address"	
     * }
     *
     * @param json
     * @return
     */
    protected static final String STR_IP = "ip-address";
    protected static final String STR_MASK = "mask";
    
    private static IPv4AddressWithMask parsePrefixFromJson(String json) {
        MappingJsonFactory f = new MappingJsonFactory();
        JsonParser jp;

        IPv4Address ip = IPv4Address.NONE;
        IPv4Address mask = IPv4Address.NO_MASK;
        
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            try {
                jp = f.createParser(json);
            } catch (JsonParseException e) {
                throw new IOException(e);
            }

            jp.nextToken();
            if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected START_OBJECT");
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                    throw new IOException("Expected FIELD_NAME");
                }

                String key = jp.getCurrentName().toLowerCase().trim();
                jp.nextToken();
                String value = jp.getText().toLowerCase().trim();
                if (value.isEmpty() || key.isEmpty()) {
                    continue;
                } else if (key.equals(STR_IP)) {
                    try {
                        ip = IPv4Address.of(value);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid IPv4 address {}", value);
                    }
                } else if (key.equals(STR_MASK)) {
                    try {
                        mask = IPv4Address.of(value);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid IPv4 address for mask {}", value);
                    }
                } 
            }
        } catch (IOException e) {
            log.error("Error parsing JSON into Server {}", e);
        }

        if (!ip.equals(IPv4Address.NONE)
                && !mask.equals(IPv4Address.NO_MASK)) {
            return IPv4AddressWithMask.of(ip, mask);
        } else {
            return null;
        }
    }
    
}
