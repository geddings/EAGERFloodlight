package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by geddingsbarrineau on 7/14/16.
 */
public class Randomizer implements IOFMessageListener, IFloodlightModule {

    //================================================================================
    //region Properties
    private ScheduledExecutorService executorService;
    private IFloodlightProviderService floodlightProvider;
    private IStaticEntryPusherService staticEntryPusherService;
    private static Logger log;

    private List<IPv4Address> whiteListedHostsIPv4;
    private List<IPv6Address> whiteListedHostsIPv6;
    //endregion
    //================================================================================


    //================================================================================
    //region Helper Functions
    private void insertInboundFlows() {};

    private void insertOutboundFlows() {};

    private IPv4Address generateRandomIPv4Address() {
        int minutes = LocalDateTime.now().getMinute();
        return IPv4Address.of(10, 0, 0, minutes);
    }

    private IPv6Address generateRandomIPv6Address() {
        return IPv6Address.of(new Random().nextLong(), new Random().nextLong());
    }

    private void startTest() {
        executorService.scheduleAtFixedRate((Runnable) () -> {
            log.info("{}", generateRandomIPv4Address());
            log.info("{}", generateRandomIPv6Address());
        }, 0L, 5L, TimeUnit.SECONDS);

        whiteListedHostsIPv4.add(IPv4Address.of(10, 0, 0, 2));
    }
    //endregion
    //================================================================================


    //================================================================================
    //region IOFMessageListener Implementation
    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
        Ethernet l2 = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        if (l2.getEtherType() == EthType.IPv4) {
            IPv4 l3 = (IPv4) l2.getPayload();
            if (whiteListedHostsIPv4.contains(l3.getDestinationAddress())) {
                log.info("Got IPv4 packet with whitelisted destination address {}", l3.getDestinationAddress());
                OFFactory factory = sw.getOFFactory();
                OFFlowAdd.Builder flow = factory.buildFlowAdd();
                Match.Builder match = factory.buildMatch();
                ArrayList<OFAction> actionList = new ArrayList<>();

                // TODO Match on more fields.
                match.setExact(MatchField.IN_PORT, inPort);
                match.setExact(MatchField.ETH_TYPE, EthType.IPv4);
                match.setExact(MatchField.IPV4_SRC, l3.getDestinationAddress());

                actionList.add(factory.actions().setNwDst(generateRandomIPv4Address()));

                flow.setBufferId(OFBufferId.NO_BUFFER);
                flow.setOutPort(OFPort.ANY);
                flow.setActions(actionList);
                flow.setMatch(match.build());
                flow.setPriority(32767);
                flow.setIdleTimeout(30);

                staticEntryPusherService.addFlow("Randomize", flow.build(), sw.getId());

//                actionList.add(factory.actions().buildOutput()
//                .setPort(OFPort.of(pi.getInPort().getPortNumber()))
//                .build());
//
//                flow.setMatch(factory.buildMatch()
//                        .setExact(MatchField.IN_PORT, OFPort.of(pi.getInPort().getPortNumber()))
//                        .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//                        .build()
//                ).setActions(actionList)
//                        .setOutPort(OFPort.ANY)
//                        .setBufferId(OFBufferId.NO_BUFFER)
//                        .build();
//
//                sw.write(flow);


                return Command.STOP;
            } else if (whiteListedHostsIPv4.contains(l3.getSourceAddress())) {
                log.info("Got IPv4 packet with whitelisted source address {}", l3.getSourceAddress());
                return Command.STOP;
            }
        }
        return Command.CONTINUE;
    }

    @Override
    public String getName() {
        return Randomizer.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        if (type.equals(OFType.PACKET_IN) && (name.equals("forwarding"))) {
            log.trace("Randomizer is telling Forwarding to run later.");
            return true;
        } else {
            return false;
        }
    }
    //endregion
    //================================================================================


    //================================================================================
    //region IFloodlightModule Implementation
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        executorService = Executors.newSingleThreadScheduledExecutor();
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        staticEntryPusherService = context.getServiceImpl(IStaticEntryPusherService.class);
        log = LoggerFactory.getLogger(Randomizer.class);

        whiteListedHostsIPv4 = new ArrayList<>();
        whiteListedHostsIPv6 = new ArrayList<>();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);

        startTest();
    }
    //endregion
    //================================================================================


}
