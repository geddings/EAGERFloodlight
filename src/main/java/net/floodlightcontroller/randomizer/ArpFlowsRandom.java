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
import org.projectfloodlight.openflow.types.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by geddingsbarrineau on 9/14/16.
 *
 * Arp flows object to encrypt and decrypt arp packets.
 */
public class ArpFlowsRandom extends AbstractFlow {

    public ArpFlowsRandom(OFPort wanport, OFPort localport, DatapathId dpid) {
        super(wanport, localport, dpid);
        log = LoggerFactory.getLogger(ArpFlowsRandom.class);
    }

    @Override
    public void insertFlow(Server server) {
        IOFSwitch sw = Randomizer.switchService.getActiveSwitch(dpid);
        ArpReplyEncrypt.insertFlow(server, sw, wanport);
        ArpRequestDecrypt.insertFlow(server, sw, localport);
    }

    @Override
    public void removeFlow(Server server) {

    }

    private static class ArpReplyEncrypt {
        static void insertFlow(Server server, IOFSwitch sw, OFPort out) {
            OFFactory factory = sw.getOFFactory();

            Match match = factory.buildMatch()
                    //.setExact(MatchField.IN_PORT, inPort)
                    .setExact(MatchField.ETH_TYPE, EthType.ARP)
                    .setExact(MatchField.ARP_SPA, server.getiPv4AddressReal())
                    .build();

            ArrayList<OFAction> actionList = new ArrayList<>();
            OFActions actions = factory.actions();
            OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
            OFActionSetField setArpSpa = actions.buildSetField()
                    .setField(
                            oxms.buildArpSpa()
                                    .setValue(server.getiPv4AddressFake())
                                    .build()
                    )
                    .build();
            actionList.add(setArpSpa);

                /* Output to a port is also an OFAction, not an OXM. */
            OFActionOutput output = actions.buildOutput()
                    .setMaxLen(0xFFffFFff)
                    .setPort(out)
                    .build();
            actionList.add(output);

            OFFlowAdd flowAdd = factory.buildFlowAdd()
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setHardTimeout(10)
                    .setIdleTimeout(10)
                    .setPriority(32768)
                    .setMatch(match)
                    .setActions(actionList)
                    //.setTableId(TableId.of(1))
                    .build();

            sw.write(flowAdd);
        }
    }

    private static class ArpRequestDecrypt {
        static void insertFlow(Server server, IOFSwitch sw, OFPort out) {
            OFFactory factory = sw.getOFFactory();

            Match match = factory.buildMatch()
                    //.setExact(MatchField.IN_PORT, inPort)
                    .setExact(MatchField.ETH_TYPE, EthType.ARP)
                    .setMasked(MatchField.ARP_TPA, server.getPrefix())
                    .build();

            ArrayList<OFAction> actionList = new ArrayList<>();
            OFActions actions = factory.actions();
            OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
            OFActionSetField setArpTpa = actions.buildSetField()
                    .setField(
                            oxms.buildArpTpa()
                                    .setValue(server.getiPv4AddressReal())
                                    .build()
                    )
                    .build();
            actionList.add(setArpTpa);

                /* Output to a port is also an OFAction, not an OXM. */
            OFActionOutput output = actions.buildOutput()
                    .setMaxLen(0xFFffFFff)
                    .setPort(out)
                    .build();
            actionList.add(output);

            OFFlowAdd flowAdd = factory.buildFlowAdd()
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setHardTimeout(10)
                    .setIdleTimeout(10)
                    .setPriority(32768)
                    .setMatch(match)
                    .setActions(actionList)
                    //.setTableId(TableId.of(1))
                    .build();

            sw.write(flowAdd);
        }
    }
}
