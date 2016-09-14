package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 9/13/16.
 */
abstract class AbstractFlow {

    protected DatapathId dpid;
    OFPort wanport = OFPort.of(1);
    OFPort hostport = OFPort.of(2);
    //protected static boolean LOCAL_HOST_IS_RANDOMIZED = false;
    protected static Logger log;

    /* Flow properties */
    int hardtimeout = 30;
    int idletimeout = 30;
    int flowpriority = 32768;

    public AbstractFlow(OFPort wanport, OFPort hostport) {
        this.wanport = wanport;
        this.hostport = hostport;
        log = LoggerFactory.getLogger(AbstractFlow.class);
    }


    abstract public void insertFlow(Server server);

}


