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
            mb = mb.setExact(MatchField.IPV4_SRC, connection.getSource().getAddressForMatch(connection.getDirection()));
            mb = mb.setExact(MatchField.IPV4_DST, connection.getDestination().getAddressForMatch(connection.getDirection()));
        } else if (ethType == EthType.ARP) {
            mb = mb.setExact(MatchField.ARP_SPA, connection.getSource().getAddressForMatch(connection.getDirection()));
            mb = mb.setExact(MatchField.ARP_TPA, connection.getDestination().getAddressForMatch(connection.getDirection()));
        }
        return mb.build();
    }

    private List<OFAction> getActionList(EthType ethType) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        actionList.addAll(getRewriteActions(ethType));
        actionList.add(getOutputPortAction());
        return actionList;
    }

    private List<OFAction> getRewriteActions(EthType ethType) {
        OFOxms oxms = factory.oxms();
        OFOxm oxm = null;
        List<OFAction> rewriteActions = new ArrayList<>();

        IPv4Address source = connection.getSource().getAddressForAction(connection.getDirection());
        IPv4Address destination = connection.getDestination().getAddressForAction(connection.getDirection());

        if (source != null) {
            if (ethType == EthType.IPv4) {
                oxm = oxms.buildIpv4Src().setValue(source).build();
                rewriteActions.add(factory.actions().buildSetField().setField(oxm).build());
            } else if (ethType == EthType.ARP) {
                oxm = oxms.buildArpSpa().setValue(source).build();
                rewriteActions.add(factory.actions().buildSetField().setField(oxm).build());
            }
        }
        
        if (destination != null) {
            if (ethType == EthType.IPv4) {
                oxm = oxms.buildIpv4Dst().setValue(destination).build();
                rewriteActions.add(factory.actions().buildSetField().setField(oxm).build());
            } else if (ethType == EthType.ARP) {
                oxm = oxms.buildArpTpa().setValue(destination).build();
                rewriteActions.add(factory.actions().buildSetField().setField(oxm).build());
            }
        }
        
        return rewriteActions;
    }

    private OFAction getOutputPortAction() {
        OFPort port = (connection.getDirection() == Connection.Direction.OUTGOING) ? wanport : lanport;
        return factory.actions().buildOutput().setMaxLen(0xFFffFFff).setPort(port).build();
    }
}
