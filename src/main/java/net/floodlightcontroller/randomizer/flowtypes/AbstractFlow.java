package net.floodlightcontroller.randomizer.flowtypes;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.randomizer.Server;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by geddingsbarrineau on 9/13/16.
 */
public abstract class AbstractFlow {

    OFPort wanport = OFPort.of(1);
    OFPort localport = OFPort.of(2);
    protected static Logger log;

    /* Flow properties */
    private Match match = null;
    private ArrayList<OFAction> action = null;
    private int hardtimeout = 30;
    private int idletimeout = 30;
    private int flowpriority = 32768;

    AbstractFlow(OFPort wanport, OFPort localport) {
        this.wanport = wanport;
        this.localport = localport;
        log = LoggerFactory.getLogger(AbstractFlow.class);
    }

    public void insertFlow(IOFSwitch sw, Server server) {
        log.debug("Inserting flow on {}.", sw.getId());
        OFFactory factory = sw.getOFFactory();
        sw.write(createFlowAdd(server, factory));
    }

    public void removeFlow(IOFSwitch sw, Server server) {
        log.debug("Removing flow on {}.", sw.getId());
        OFFactory factory = sw.getOFFactory();
        sw.write(createFlowDelete(server, factory));
    }

    private OFFlowDeleteStrict createFlowDelete(Server server, OFFactory factory) {
        return factory.buildFlowDeleteStrict()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(hardtimeout)
                .setIdleTimeout(idletimeout)
                .setPriority(flowpriority)
                .setMatch(match)
                .setActions(action)
                .build();
    }

    private OFFlowAdd createFlowAdd(Server server, OFFactory factory) {
        this.match = createMatches(server, factory);
        this.action = createActions(server, factory);
        return factory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(hardtimeout)
                .setIdleTimeout(idletimeout)
                .setPriority(flowpriority)
                .setMatch(match)
                .setActions(action)
                .build();
    }

    abstract Match createMatches(Server server, OFFactory factory);

    abstract ArrayList<OFAction> createActions(Server server, OFFactory factory);

    @Override
    public String toString() {
        return "AbstractFlow{" +
                ", wanport=" + wanport +
                ", localport=" + localport +
                ", match=" + match +
                ", action=" + action +
                '}';
    }
}


