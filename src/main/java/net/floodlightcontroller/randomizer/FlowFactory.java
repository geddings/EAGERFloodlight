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
 *
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

    private Server server;

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


    FlowFactory(Server server) {
        this.server = server;
    }

    // FIXME: This needs to be refactored to return the specific OFFlowAdd type.
    List<OFFlowMod> getFlowAdds() {
        return getFlows(OFFlowModCommand.ADD);
    }

    /**
     * Given a server, this function returns a list of flows to be inserted on the switch.
     * This list of flows should contain all encrypt and decrypt flows for ARP and IP.
     * @param flowModCommand
     * @return list of flowMods
     */
    // TODO: We are assuming OpenFlow 1.3 right now. This should be extended to handle any version.
    List<OFFlowMod> getFlows(OFFlowModCommand flowModCommand) {
        List<OFFlowMod> flows = new ArrayList<>();

        flows.addAll(getEncryptFlows(flowModCommand));
        flows.addAll(getDecryptFlows(flowModCommand));

        return flows;
    }

    private List<OFFlowMod> getEncryptFlows(OFFlowModCommand fmc) {
        List<OFFlowMod> encryptFlows = new ArrayList<>();
        encryptFlows.add(getFlow(new RewriteFlow(FlowType.ENCRYPT, EthType.IPv4), fmc));
        encryptFlows.add(getFlow(new RewriteFlow(FlowType.ENCRYPT, EthType.ARP), fmc));
        return encryptFlows;
    }

    private List<OFFlowMod> getDecryptFlows(OFFlowModCommand fmc) {
        List<OFFlowMod> decryptFlows = new ArrayList<>();
        decryptFlows.add(getFlow(new RewriteFlow(FlowType.DECRYPT, EthType.IPv4), fmc));
        decryptFlows.add(getFlow(new RewriteFlow(FlowType.DECRYPT, EthType.ARP), fmc));
        return decryptFlows;
    }

    private OFFlowMod getFlow(RewriteFlow flow, OFFlowModCommand flowModCommand) {
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
                .setMatch(getMatch(flow))
                .setActions(getActionList(flow))
                .build();
    }

    protected Match getMatch(RewriteFlow flow) {
        Match.Builder mb = factory.buildMatch();
        mb = mb.setExact(MatchField.ETH_TYPE, flow.ethType);

        MatchField<IPv4Address> mf = null;

        boolean encrypt = (flow.flowType == FlowType.ENCRYPT);
        if (flow.ethType == EthType.IPv4) {
            mf = (Boolean.logicalXor(randomize, encrypt)) ? MatchField.IPV4_DST : MatchField.IPV4_SRC;
        }
        else if (flow.ethType == EthType.ARP) {
            mf = (Boolean.logicalXor(randomize, encrypt)) ? MatchField.ARP_TPA : MatchField.ARP_SPA;
        }
        IPv4Address ip = getMatchIPAddress(flow.flowType);
        mb = mb.setExact(mf, ip);
        return mb.build();
    }

    private IPv4Address getMatchIPAddress(FlowType flowType) {
        return (flowType == FlowType.ENCRYPT) ? server.getiPv4AddressReal() : server.getiPv4AddressFake();
    }

    private List<OFAction> getActionList(RewriteFlow flow) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        actionList.add(getRewriteAction(flow));
        actionList.add(getOutputPortAction(flow.flowType));
        return actionList;
    }

    private OFAction getRewriteAction(RewriteFlow flow) {
        OFOxms oxms = factory.oxms();
        OFOxm oxm = null;
        boolean encrypt = (flow.flowType == FlowType.ENCRYPT);

        IPv4Address ip = getRewriteActionIPAddress(flow.flowType);
        if (flow.ethType == EthType.IPv4) {
            oxm = (Boolean.logicalXor(randomize, encrypt))
                    ? oxms.buildIpv4Dst().setValue(ip).build()
                    : oxms.buildIpv4Src().setValue(ip).build();
        }
        else if (flow.ethType == EthType.ARP) {
            oxm = (Boolean.logicalXor(randomize, encrypt))
                    ? oxms.buildArpTpa().setValue(ip).build()
                    : oxms.buildArpSpa().setValue(ip).build();
        }
        return factory.actions().buildSetField().setField(oxm).build();
    }

    private IPv4Address getRewriteActionIPAddress(FlowType flowType) {
        return (flowType == FlowType.ENCRYPT) ? server.getiPv4AddressFake() : server.getiPv4AddressReal();
    }

    private OFAction getOutputPortAction(FlowType flowType) {
        OFPort port = (flowType == FlowType.ENCRYPT) ? wanport : lanport;
        return factory.actions().buildOutput().setMaxLen(0xFFffFFff).setPort(port).build();
    }

    protected static class RewriteFlow {
        FlowType flowType;
        EthType ethType;

        RewriteFlow(FlowType flowType, EthType ethType) {
            this.flowType = flowType;
            this.ethType = ethType;
        }
    }
}
