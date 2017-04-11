package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.*;

import java.util.*;

/**
 * The Flow Factory is intended to take all responsibility for creating
 * the correct matches and actions for all the different types of flows
 * needed for the Randomizer.
 * <p>
 * Created by geddingsbarrineau on 12/13/16.
 */
public class FlowFactory {

    protected enum FlowType {
        ENCRYPT, DECRYPT
    }

    private static boolean randomize = true;
    private static OFPort wanport = OFPort.of(1);
    private static OFPort lanport = OFPort.of(2);
    private OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
    private int hardtimeout = 30;
    private int idletimeout = 30;
    private int flowpriority = 32768;

    private Connection connection;

    /* Experimental stuff */
    public static boolean isRandomize() {
        return randomize;
    }

    static void setRandomize(boolean randomize) {
        FlowFactory.randomize = randomize;
    }

    public static OFPort getWanport() {
        return wanport;
    }

    public static void setWanport(OFPort wanport) {
        FlowFactory.wanport = wanport;
    }

    public static OFPort getLanport() {
        return lanport;
    }

    static void setLanport(OFPort lanport) {
        FlowFactory.lanport = lanport;
    }


    FlowFactory(Connection connection) {
        this.connection = connection;
    }

    // FIXME: This needs to be refactored to return the specific OFFlowAdd type.
    List<OFFlowMod> getFlowAdds() {
        return getFlows(OFFlowModCommand.ADD);
    }

    /**
     * Given a server, this function returns a list of flows to be inserted on the switch.
     * This list of flows should contain all encrypt and decrypt flows for ARP and IP.
     *
     * @param fmc The flow mod command that you want to create flows for e.g. Add, Delete, etc.
     * @return list of flowMods
     */
    // TODO: We are assuming OpenFlow 1.3 right now. This should be extended to handle any version.
    private List<OFFlowMod> getFlows(OFFlowModCommand fmc) {
        List<OFFlowMod> flows = new ArrayList<>();

        flows.add(getFlow(EthType.IPv4, fmc));
        flows.add(getFlow(EthType.ARP, fmc));

        return flows;
    }

    private OFFlowMod getFlow(EthType ethType, OFFlowModCommand flowModCommand) {
        OFFlowMod.Builder fmb;
        switch (flowModCommand) {
            case ADD:
                fmb = factory.buildFlowAdd();
                break;
            case DELETE:
                fmb = factory.buildFlowDelete();
                break;
            case DELETE_STRICT:
                fmb = factory.buildFlowDeleteStrict();
                break;
            case MODIFY:
                fmb = factory.buildFlowModify();
                break;
            case MODIFY_STRICT:
                fmb = factory.buildFlowModifyStrict();
                break;
            default:
                // FIXME: This needs to be handled properly.
                return null;
        }
        return fmb.setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(hardtimeout)
                .setIdleTimeout(idletimeout)
                .setPriority(flowpriority)
                .setMatch(getMatch(ethType))
                .setActions(getActionList(ethType))
                .build();
    }

    protected Match getMatch(EthType ethType) {
        Match.Builder mb = factory.buildMatch();
        mb = mb.setExact(MatchField.ETH_TYPE, ethType);

        if (ethType == EthType.IPv4) {
            mb = mb.setExact(MatchField.IPV4_SRC, getMatchIPAddress(connection.getSource()));
            mb = mb.setExact(MatchField.IPV4_DST, getMatchIPAddress(connection.getDestination()));
        } else if (ethType == EthType.ARP) {
            mb = mb.setExact(MatchField.ARP_SPA, getMatchIPAddress(connection.getSource()));
            mb = mb.setExact(MatchField.ARP_TPA, getMatchIPAddress(connection.getDestination()));
        }
        return mb.build();
    }

    private IPv4Address getMatchIPAddress(Host host) {
        return (connection.getDirection() == Connection.Direction.INCOMING) ? host.getExternalIP() : host.getInternalIP();
    }

    private List<OFAction> getActionList(EthType ethType) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        actionList.add(getRewriteAction(ethType));
        actionList.add(getOutputPortAction());
        return actionList;
    }

    private OFAction getRewriteAction(EthType ethType) {
        OFOxms oxms = factory.oxms();
        OFOxm oxm = null;
        boolean encrypt = (flow.flowType == FlowType.ENCRYPT);

        IPv4Address ip = getRewriteActionIPAddress(flow.flowType);
        if (flow.ethType == EthType.IPv4) {
            oxm = (Boolean.logicalXor(randomize, encrypt))
                    ? oxms.buildIpv4Dst().setValue(ip).build()
                    : oxms.buildIpv4Src().setValue(ip).build();
        } else if (flow.ethType == EthType.ARP) {
            oxm = (Boolean.logicalXor(randomize, encrypt))
                    ? oxms.buildArpTpa().setValue(ip).build()
                    : oxms.buildArpSpa().setValue(ip).build();
        }
        return factory.actions().buildSetField().setField(oxm).build();
    }
    
    private OFOxm getRewriteAction(Host host) {
        OFOxm oxm;
        
        if (host.isRandomized()) {
            
        }
    }

    private IPv4Address getRewriteActionIPAddress(Host host) {
        return (connection.getDirection() == Connection.Direction.INCOMING) ? host.getInternalIP() : host.getExternalIP();
    }

    private OFAction getOutputPortAction() {
        OFPort port = (connection.getDirection() == Connection.Direction.OUTGOING) ? wanport : lanport;
        return factory.actions().buildOutput().setMaxLen(0xFFffFFff).setPort(port).build();
    }
}
