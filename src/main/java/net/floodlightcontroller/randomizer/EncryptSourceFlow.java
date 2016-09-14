package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by geddingsbarrineau on 9/14/16.
 */
public class EncryptSourceFlow extends AbstractFlow {

    private static Logger log;

    public EncryptSourceFlow(OFPort wanport, OFPort hostport) {
        super(wanport, hostport);
        log = LoggerFactory.getLogger(EncryptSourceFlow.class);
    }

    @Override
    public void insertFlow(Server server) {
        IOFSwitch sw = Randomizer.switchService.getActiveSwitch(dpid);
        OFFactory factory = sw.getOFFactory();
        sw.write(createFlowAdd(server, factory));
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
                .setExact(MatchField.IPV4_SRC, server.getiPv4AddressReal())
                .build();
    }

    private ArrayList<OFAction> createActions(Server server, OFFactory factory) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwSrc = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Src()
                                .setValue(server.getiPv4AddressFake())
                                .build()
                )
                .build();
        actionList.add(setNwSrc);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(wanport)
                .build();
        actionList.add(output);
        return actionList;
    }
}
