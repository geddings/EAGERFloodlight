package net.floodlightcontroller.randomizer.web;

import net.floodlightcontroller.randomizer.IRandomizerService;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by geddingsbarrineau on 9/21/16.
 */
public class ServerResource extends org.restlet.resource.ServerResource {
    protected static Logger log = LoggerFactory.getLogger(ServerResource.class);
    protected static final String STR_OPERATION_ADD = "add";
    protected static final String STR_OPERATION_REMOVE = "remove";

    protected static final String STR_IP = "ip-address";
    protected static final String STR_SEED = "seed";

    @Get
    public Object getServers() {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());

        return randomizerService.getServers();
    }

    @Put
    @Post
    public Map<String, String> handleServer(String json) {

    }
}
