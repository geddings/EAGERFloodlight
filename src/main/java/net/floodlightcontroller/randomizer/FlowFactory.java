package net.floodlightcontroller.randomizer;

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
public class FlowFactory extends AbstractFlowFactory {

    private Connection connection;

    FlowFactory(Connection connection) {
        this.connection = connection;
    }

    protected Match getMatch(EthType ethType) {
        Match.Builder mb = factory.buildMatch();
        mb = mb.setExact(MatchField.ETH_TYPE, ethType);

        if (ethType == EthType.IPv4) {
            mb = mb.setExact(MatchField.IPV4_SRC, getMatchAddress(connection.getSource(), connection.getDirection()));
            mb = mb.setExact(MatchField.IPV4_DST, getMatchAddress(connection.getDestination(), connection.getDirection()));
        } else if (ethType == EthType.ARP) {
            mb = mb.setExact(MatchField.ARP_SPA, getMatchAddress(connection.getSource(), connection.getDirection()));
            mb = mb.setExact(MatchField.ARP_TPA, getMatchAddress(connection.getDestination(), connection.getDirection()));
        }
        return mb.build();
    }

    protected IPv4Address getMatchAddress(Host host, Direction direction) {
        if (host instanceof RandomizedHost && direction == Direction.INCOMING) {
            return ((RandomizedHost)host).getRandomizedAddress();
        }
        else {
            return host.getAddress();
        }
    }

    protected List<OFAction> getActions(EthType ethType) {
        ArrayList<OFAction> actionList = new ArrayList<>();
        actionList.addAll(getRewriteActions(ethType));
        actionList.add(getOutputPortAction());
        return actionList;
    }

    private List<OFAction> getRewriteActions(EthType ethType) {
        OFOxms oxms = factory.oxms();
        OFOxm oxm = null;
        List<OFAction> rewriteActions = new ArrayList<>();

        IPv4Address source = getActionAddress(connection.getSource(), connection.getDirection());
        IPv4Address destination = getActionAddress(connection.getDestination(), connection.getDirection());

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

    private IPv4Address getActionAddress(Host host, Direction direction) {
        if (host instanceof RandomizedHost) {
            if (direction == Direction.OUTGOING) {
                return ((RandomizedHost) host).getRandomizedAddress();
            } else {
                return host.getAddress();
            }
        }
        return null;
    }

    private OFAction getOutputPortAction() {
        OFPort port = (connection.getDirection() == Direction.OUTGOING) ? wanport : lanport;
        return factory.actions().buildOutput().setMaxLen(0xFFffFFff).setPort(port).build();
    }
}
