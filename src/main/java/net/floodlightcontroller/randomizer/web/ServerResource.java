package net.floodlightcontroller.randomizer.web;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import net.floodlightcontroller.randomizer.IRandomizerService;
import net.floodlightcontroller.randomizer.IRandomizerService.RandomizerReturnCode;
import net.floodlightcontroller.randomizer.Server;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by geddingsbarrineau on 9/21/16.
 */
public class ServerResource extends org.restlet.resource.ServerResource {
    protected static Logger log = LoggerFactory.getLogger(ServerResource.class);
    protected static final String STR_OPERATION_ADD = "add";
    protected static final String STR_OPERATION_REMOVE = "remove";

    protected static final String STR_SERVER = "server";

    @Get
    public Object getServers() {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());
        return randomizerService.getServers();
    }

    @Put
    @Post
    public Map<String, String> handleServer(String json) {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());
        String operation = (String) getRequestAttributes().get("operation");

        Map<String, String> ret = new HashMap<String, String>();

        Server server = parseServerFromJson(json);
        if (server == null) {
            ret.put(Code.CODE, Code.ERR_JSON);
            ret.put(Code.MESSAGE, "Error: Could not parse JSON.");
        } else if (operation.equals(STR_OPERATION_ADD)) {
            RandomizerReturnCode rc = randomizerService.addServer(server);
            switch (rc) {
                case SERVER_ADDED:
                    ret.put(Code.CODE, Code.OKAY);
                    ret.put(Code.MESSAGE, "Server successfully added. It will be available for the next Randomizer session.");
                    break;
                case ERR_DUPLICATE_SERVER:
                    ret.put(Code.CODE, Code.ERR_DUPLICATE);
                    ret.put(Code.MESSAGE, "Error: A duplicate server was detected. Unable to add server to Randomizer.");
                    break;
                default:
                    ret.put(Code.CODE, Code.ERR_BAD_ERR_CODE);
                    ret.put(Code.MESSAGE, "Error: Unexpected error code " + rc.toString() + ". Server was not added.");
                    break;
            }
        } else if (operation.equals(STR_OPERATION_REMOVE)) {
            RandomizerReturnCode rc = randomizerService.removeServer(server);
            switch (rc) {
                case SERVER_REMOVED:
                    ret.put(Code.CODE, Code.OKAY);
                    ret.put(Code.MESSAGE, "Server successfully removed. It will be no longer be available for the next Randomizer session.");
                    break;
                case ERR_UNKNOWN_SERVER:
                    ret.put(Code.CODE, Code.ERR_NOT_FOUND);
                    ret.put(Code.MESSAGE, "Error: The server specified was not found. Unable to remove server from Randomizer.");
                    break;
                default:
                    ret.put(Code.CODE, Code.ERR_BAD_ERR_CODE);
                    ret.put(Code.MESSAGE, "Error: Unexpected error code " + rc.toString() + ". Server was not removed.");
                    break;
            }
        } else {
            ret.put(Code.CODE, Code.ERR_UNDEF_OPERATION);
            ret.put(Code.MESSAGE, "Error: Undefined operation " + operation);
        }

        return ret;
    }

    /**
     * Expect JSON:
     * {
     * 		"server"	:	"valid-ip-address",
     * }
     *
     * @param json
     * @return
     */
    private static Server parseServerFromJson(String json) {
        MappingJsonFactory f = new MappingJsonFactory();
        JsonParser jp;

        IPv4Address ip = IPv4Address.NONE;
        IPv4AddressWithMask prefix = IPv4AddressWithMask.NONE;

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
                } else if (key.equals(STR_SERVER)) {
                    try {
                        ip = IPv4Address.of(value);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid IPv4 address {}", value);
                    }
                } 
            }
        } catch (IOException e) {
            log.error("Error parsing JSON into Server {}", e);
        }

        if (!ip.equals(IPv4Address.NONE)) {
            return new Server(ip);
        } else {
            return null;
        }
    }

}
