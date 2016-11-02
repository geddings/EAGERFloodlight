package net.floodlightcontroller.randomizer.web;

import net.floodlightcontroller.randomizer.IRandomizerService;
import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 11/2/16.
 *
 */
public class ConnectionsResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(InfoResource.class);

    @Get
    public Object getConnections() {
        IRandomizerService randomizerService = (IRandomizerService) getContext().getAttributes().get(IRandomizerService.class.getCanonicalName());
        return randomizerService.getConnections();
    }
}