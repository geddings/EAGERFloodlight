package net.floodlightcontroller.randomizer.flowtypes;

import net.floodlightcontroller.randomizer.Server;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by geddingsbarrineau on 9/13/16.
 *
 */
public class EncryptDestinationFlow extends AbstractFlow {

    private static Logger log;

    public EncryptDestinationFlow(OFPort wanport, OFPort localport, DatapathId dpid) {
        super(wanport, localport);
        log = LoggerFactory.getLogger(EncryptDestinationFlow.class);
    }

    protected Match createMatches(Server server, OFFactory factory) {
        return factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, server.getiPv4AddressReal())
                .build();
    }

    protected ArrayList<OFAction> createActions(Server server, OFFactory factory) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwDst = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Dst()
                                .setValue(server.getiPv4AddressFake())
                                .build()
                )
                .build();
        actionList.add(setNwDst);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(wanport)
                .build();
        actionList.add(output);
        return actionList;
    }
}
