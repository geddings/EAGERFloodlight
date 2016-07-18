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
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
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

    private static boolean LOCAL_HOST_IS_RANDOMIZED = false;


    //endregion
    //================================================================================


    //================================================================================
    //region Helper Functions
    private void insertDestinationEncryptFlow(IOFSwitch sw, IPv4 l3) {
        OFFactory factory = sw.getOFFactory();

        Match match = factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, l3.getDestinationAddress())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwDst = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Dst()
                                .setValue(generateRandomIPv4Address())
                                .build()
                )
                .build();
        actionList.add(setNwDst);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.of(1))
                .build();
        actionList.add(output);

        OFFlowAdd flowAdd = factory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(30)
                .setIdleTimeout(30)
                .setPriority(32768)
                .setMatch(match)
                .setActions(actionList)
                //.setTableId(TableId.of(1))
                .build();

        sw.write(flowAdd);
    }

    private void insertDestinationDecryptFlow(IOFSwitch sw, IPv4 l3) {
        OFFactory factory = sw.getOFFactory();

        Match match = factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, generateRandomIPv4Address()) //TODO Pull this from a map
                .build();

        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwDst = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Dst()
                                .setValue(l3.getDestinationAddress())
                                .build()
                )
                .build();
        actionList.add(setNwDst);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.LOCAL)
                .build();
        actionList.add(output);

        OFFlowAdd flowAdd = factory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(30)
                .setIdleTimeout(30)
                .setPriority(32768)
                .setMatch(match)
                .setActions(actionList)
                //.setTableId(TableId.of(1))
                .build();

        sw.write(flowAdd);}

    private void insertSourceEncryptFlow(IOFSwitch sw, IPv4 l3) {
        OFFactory factory = sw.getOFFactory();

        Match match = factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, l3.getSourceAddress())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwSrc = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Src()
                                .setValue(generateRandomIPv4Address())
                                .build()
                )
                .build();
        actionList.add(setNwSrc);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.of(1))
                .build();
        actionList.add(output);

        OFFlowAdd flowAdd = factory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(30)
                .setIdleTimeout(30)
                .setPriority(32768)
                .setMatch(match)
                .setActions(actionList)
                //.setTableId(TableId.of(1))
                .build();

        sw.write(flowAdd);}

    private void insertSourceDecryptFlow(IOFSwitch sw, IPv4 l3) {
        OFFactory factory = sw.getOFFactory();

        Match match = factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, generateRandomIPv4Address())
                .build();

        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwSrc = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Src()
                                .setValue(l3.getSourceAddress())
                                .build()
                )
                .build();
        actionList.add(setNwSrc);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(OFPort.LOCAL)
                .build();
        actionList.add(output);

        OFFlowAdd flowAdd = factory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(30)
                .setIdleTimeout(30)
                .setPriority(32768)
                .setMatch(match)
                .setActions(actionList)
                //.setTableId(TableId.of(1))
                .build();

        sw.write(flowAdd);}

    private IPv4Address generateRandomIPv4Address() {
//        int minutes = LocalDateTime.now().getMinute();
        int seconds = LocalDateTime.now().getSecond();
        return IPv4Address.of(10, 0, 0, seconds);
    }

    private IPv6Address generateRandomIPv6Address() {
        return IPv6Address.of(new Random().nextLong(), new Random().nextLong());
    }

    private void startTest() {
        executorService.scheduleAtFixedRate((Runnable) () -> {
            log.info("{}", generateRandomIPv4Address());
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
            if (LOCAL_HOST_IS_RANDOMIZED) {
                log.info("IPv4 packet seen on Randomized host.");
                if (inPort.equals(OFPort.LOCAL)) {
                    log.info("LOCAL! Inserting flow to encrypt the source IP.");
                    insertSourceEncryptFlow(sw, l3);
                } else {
                    log.info("Other! Inserting flow to decrypt the destination IP.");
                    insertDestinationDecryptFlow(sw, l3);
                }
            } else {
                log.info("IPv4 packet seen on non-Randomized host.");
                if (inPort.equals(OFPort.LOCAL)) {
                    log.info("LOCAL! Inserting flow to encrypt the destination IP.");
                    insertDestinationEncryptFlow(sw, l3);
                } else {
                    log.info("Other! Inserting flow to decrypt the source IP.");
                    insertSourceDecryptFlow(sw, l3);
                }
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

        Map<String, String> configParameters = context.getConfigParams(this);
        String tmp = configParameters.get("randomize-host");
        if (tmp != null) {
            tmp = tmp.trim().toLowerCase();
            if (tmp.contains("yes") && tmp.contains("true")) {
                LOCAL_HOST_IS_RANDOMIZED = true;
                log.info("Local host will be treated as having randomized addresses.");
            } else {
                LOCAL_HOST_IS_RANDOMIZED = false;
                log.info("Local host will be treated as having a static address.");
            }
        }


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
