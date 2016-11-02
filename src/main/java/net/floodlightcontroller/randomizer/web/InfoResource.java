package net.floodlightcontroller.randomizer.web;

import net.floodlightcontroller.randomizer.IRandomizerService;
import net.floodlightcontroller.randomizer.Server;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by geddingsbarrineau on 10/29/16.
 *
 */
public class InfoResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(InfoResource.class);

    public static class InfoJsonSerializerWrapper {
        private final String prefix;
        private final List<Server> servers;

        public InfoJsonSerializerWrapper(String prefix, List<Server> servers) {
            this.prefix = prefix;
            this.servers = servers;
        }
    }

    @Get
    public Map<String, String> getEAGERInfo() {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());
        Map<String, String> ret = new HashMap<>();
        ret.put("current-prefix", randomizerService.getCurrentPrefix().toString());
        return ret;
    }
}
