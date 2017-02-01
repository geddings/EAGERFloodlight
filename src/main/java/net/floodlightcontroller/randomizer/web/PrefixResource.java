package net.floodlightcontroller.randomizer.web;

import net.floodlightcontroller.randomizer.IRandomizerService;
import org.projectfloodlight.openflow.types.IPAddressWithMask;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            randomizerService.addPrefix(IPv4AddressWithMask.of(json));
            return Collections.singletonMap("SUCCESS", "Prefix added!");
        }
        
        if (operation.equals(STR_OPERATION_REMOVE)) {
            randomizerService.removePrefix(IPv4AddressWithMask.of(json));
            return Collections.singletonMap("SUCCESS", "Prefix removed!");
        }

        return Collections.singletonMap("ERROR", "Unimplemented configuration option");
    }
    
}
