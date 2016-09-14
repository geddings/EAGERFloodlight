package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by geddingsbarrineau on 9/14/16.
 */
public class DecryptDestinationFlow extends AbstractFlow {

    private static Logger log;

    public DecryptDestinationFlow(OFPort wanport, OFPort hostport, DatapathId dpid) {
        super(wanport, hostport, dpid);
        log = LoggerFactory.getLogger(DecryptDestinationFlow.class);
    }

    @Override
    public void insertFlow(Server server) {
        IOFSwitch sw = Randomizer.switchService.getActiveSwitch(dpid);
        OFFactory factory = sw.getOFFactory();
        sw.write(createFlowAdd(server, factory));
    }

    @Override
    public void removeFlow(Server server) {
        IOFSwitch sw = Randomizer.switchService.getActiveSwitch(dpid);
        OFFactory factory = sw.getOFFactory();
        sw.write(createFlowDelete(server, factory));
    }

    private OFFlowDeleteStrict createFlowDelete(Server server, OFFactory factory) {
        return factory.buildFlowDeleteStrict()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(hardtimeout)
                .setIdleTimeout(idletimeout)
                .setPriority(flowpriority)
                .setMatch(createMatches(server, factory))
                .setActions(createActions(server, factory))
                //.setTableId(TableId.of(1))
                .build();
    }

    private OFFlowAdd createFlowAdd(Server server, OFFactory factory) {
        return factory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(hardtimeout)
                .setIdleTimeout(idletimeout)
                .setPriority(flowpriority)
                .setMatch(createMatches(server, factory))
                .setActions(createActions(server, factory))
                //.setTableId(TableId.of(1))
                .build();
    }

    private Match createMatches(Server server, OFFactory factory) {
        return factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, server.getiPv4AddressFake())
                .build();
    }

    private ArrayList<OFAction> createActions(Server server, OFFactory factory) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwDst = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Dst()
                                .setValue(server.getiPv4AddressReal())
                                .build()
                )
                .build();
        actionList.add(setNwDst);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(hostport)
                .build();
        actionList.add(output);
        return actionList;
    }
}
