package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 9/13/16.
 */
abstract class AbstractFlow {

    protected IOFSwitch sw;
    protected OFPort wanport = OFPort.of(1);
    //protected static boolean LOCAL_HOST_IS_RANDOMIZED = false;
    protected static Logger log;

    /* Flow properties */
    protected int hardtimeout = 30;
    protected int idletimeout = 30;
    protected int priority = 32768;

    public AbstractFlow(OFPort wanport) {
        this.wanport = wanport;
        log = LoggerFactory.getLogger(AbstractFlow.class);
    }


    abstract public void insertFlow(Server server);

}


