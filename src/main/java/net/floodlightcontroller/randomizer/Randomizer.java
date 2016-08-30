package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
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
public class Randomizer implements IOFMessageListener, IOFSwitchListener, IFloodlightModule {

    //================================================================================
    //region Properties
    private ScheduledExecutorService executorService;
    private IDeviceService deviceService;
    private IFloodlightProviderService floodlightProvider;
    private IOFSwitchService switchService;
    private IStaticEntryPusherService staticEntryPusherService;
    private static Logger log;
    private static Random generator;

    private List<IPv4Address> whiteListedHostsIPv4;
    private List<IPv6Address> whiteListedHostsIPv6;

    private Map<IPv4Address, IPv4Address> randomizedServerList;
    private static boolean LOCAL_HOST_IS_RANDOMIZED = false;

    private int SEED = 1234;
    //endregion
    //================================================================================


    //================================================================================
    //region Helper Functions
    private void insertDestinationEncryptFlow(IOFSwitch sw, IPv4 l3, OFPort out) {
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
                                .setValue(randomizedServerList.get(l3.getDestinationAddress()))
                                .build()
                )
                .build();
        actionList.add(setNwDst);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(out)
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

    private void insertDestinationDecryptFlow(IOFSwitch sw, IPv4 l3, OFPort out) {
        OFFactory factory = sw.getOFFactory();

        Match match = factory.buildMatch()
                //.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, l3.getDestinationAddress()) //TODO Pull this from a map
                .build();

        ArrayList<OFAction> actionList = new ArrayList<>();
        OFActions actions = factory.actions();
        OFOxms oxms = factory.oxms();

                /* Use OXM to modify network layer dest field. */
        OFActionSetField setNwDst = actions.buildSetField()
                .setField(
                        oxms.buildIpv4Dst()
                                .setValue((IPv4Address)getKeyFromValue(randomizedServerList, l3.getDestinationAddress())) // TODO Pull this from a map? Maybe have hardcoded value
                                .build()
                )
                .build();
        actionList.add(setNwDst);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(out)
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

    private void insertSourceEncryptFlow(IOFSwitch sw, IPv4 l3, OFPort out) {
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
                                .setValue(randomizedServerList.get(l3.getSourceAddress()))
                                .build()
                )
                .build();
        actionList.add(setNwSrc);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(out)
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

    private void insertSourceDecryptFlow(IOFSwitch sw, IPv4 l3, OFPort out) {
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
                                .setValue((IPv4Address)getKeyFromValue(randomizedServerList, l3.getSourceAddress())) // TODO Check for null
                                .build()
                )
                .build();
        actionList.add(setNwSrc);

                /* Output to a port is also an OFAction, not an OXM. */
        OFActionOutput output = actions.buildOutput()
                .setMaxLen(0xFFffFFff)
                .setPort(out)
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
        int minutes = LocalDateTime.now().getMinute();
        int seconds = LocalDateTime.now().getSecond();
        return IPv4Address.of(10, 0, 0, minutes);
    }

    private IPv6Address generateRandomIPv6Address() {
        return IPv6Address.of(new Random().nextLong(), new Random().nextLong());
    }

    private  void findHostIPv4(IPv4Address ipaddr) {
        Set<DatapathId> switches = switchService.getAllSwitchDpids();
        //log.warn("Agent {} does not have known/true attachment point(s). Flooding ARP on all switches", a);
        for (DatapathId sw : switches) {
            //log.trace("Agent {} does not have known/true attachment point(s). Flooding ARP on switch {}", a, sw);
            log.info("Arping for host on switch {}", sw);
            arpForDevice(
                    ipaddr,
                    (ipaddr.and(IPv4Address.of("255.255.255.0"))).or(IPv4Address.of("0.0.0.254")) /* Doesn't matter really; must be same subnet though */,
                    MacAddress.BROADCAST /* Use broadcast as to not potentially confuse a host's ARP cache */,
                    VlanVid.ZERO /* Switch will push correct VLAN tag if required */,
                    switchService.getSwitch(sw)
            );
        }
    }

    private void findHostIPv6() {}

    private void startTest() {
        executorService.scheduleAtFixedRate((Runnable) () -> {
            randomizedServerList.put(IPv4Address.of(10,0,0,4), IPv4Address.of(generator.nextInt()));
            log.info("{}", randomizedServerList);
            //findHostIPv4(IPv4Address.of(10, 0, 0, 3));
            //log.info("{}", deviceService.queryDevices(MacAddress.NONE, null, IPv4Address.of(10,0,0,3), IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO).hasNext());
        }, 0L, 20L, TimeUnit.SECONDS);

        whiteListedHostsIPv4.add(IPv4Address.of(10, 0, 0, 2));

    }

    public static Object getKeyFromValue(Map hm, Object value) {
        for (Object o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }

    /**
     * Try to force-learn a device that the device manager does not know
     * about already. The ARP reply (we hope for) will trigger learning
     * the new device, and the next TCP SYN we receive after that will
     * result in a successful device lookup in the device manager.
     * @param dstIp
     * @param srcIp
     * @param srcMac
     * @param vlan
     * @param sw
     */
    private void arpForDevice(IPv4Address dstIp, IPv4Address srcIp, MacAddress srcMac, VlanVid vlan, IOFSwitch sw) {
        IPacket arpRequest = new Ethernet()
                .setSourceMACAddress(srcMac)
                .setDestinationMACAddress(MacAddress.BROADCAST)
                .setEtherType(EthType.ARP)
                .setVlanID(vlan.getVlan())
                .setPayload(
                        new ARP()
                                .setHardwareType(ARP.HW_TYPE_ETHERNET)
                                .setProtocolType(ARP.PROTO_TYPE_IP)
                                .setHardwareAddressLength((byte) 6)
                                .setProtocolAddressLength((byte) 4)
                                .setOpCode(ARP.OP_REQUEST)
                                .setSenderHardwareAddress(srcMac)
                                .setSenderProtocolAddress(srcIp)
                                .setTargetHardwareAddress(MacAddress.NONE)
                                .setTargetProtocolAddress(dstIp));

        OFPacketOut po = sw.getOFFactory().buildPacketOut()
                .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF)))
                .setBufferId(OFBufferId.NO_BUFFER)
                .setData(arpRequest.serialize())
                .setInPort(OFPort.CONTROLLER)
                .build();
        sw.write(po);
    }

    private SwitchPort[] getAttachmentPointsForDevice(IPv4Address ipaddr, DatapathId dpid) {
        Iterator<? extends IDevice> i = deviceService.queryDevices(MacAddress.NONE, null, ipaddr, IPv6Address.NONE,
                dpid, OFPort.ZERO);
        if (i.hasNext()) {
            IDevice d = i.next();
            return d.getAttachmentPoints();
        }
        else {
            log.info("Arping for host {}", ipaddr);
            findHostIPv4(ipaddr);
            return null;
        }
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
            // Is the local host supposed to be randomized? (Defined in the properties file)
            if (LOCAL_HOST_IS_RANDOMIZED) {
                log.info("IPv4 packet seen on Randomized host. \nSrc:{}\nDst:{}\nInPort:{}", new Object[] {l3.getSourceAddress(), l3.getDestinationAddress(),
                        inPort});
                // Is this packet part of the randomized connection?
                // For randomized host, there are two things to look for:
                // 1) Is the dst a random address in use currently? If so, a flow needs to be inserted.
                // 2) Is the src a real server address? If so, a flow needs to be inserted.
                if (randomizedServerList.containsValue(l3.getDestinationAddress())) {
                    log.info("1. Packet is destined for a server's random address contained in the randomized server list.");
                    SwitchPort[] aps = getAttachmentPointsForDevice((IPv4Address)getKeyFromValue(
                            randomizedServerList, l3.getDestinationAddress()), sw.getId());
                    if (aps != null) {
                        log.info("{}", aps);
                        insertDestinationDecryptFlow(sw, l3, aps[0].getPortId()); // TODO Make ports dynamic
                    }
                    return Command.STOP;
                } else if (randomizedServerList.containsKey(l3.getSourceAddress())) {
                    log.info("2. Packet is coming from a server's real address contained in the randomized server list.");
                    insertSourceEncryptFlow(sw, l3, OFPort.of(1));
                    return Command.STOP;
                }
            } else {
                log.info("IPv4 packet seen on non-Randomized host. \nSrc:{}\nDst:{}\nInPort:{}", new Object[] {l3.getSourceAddress(), l3.getDestinationAddress(),
                        inPort});
                // Is this packet part of the randomized connection?
                // For non-randomized host, there are two things to look for:
                // 1) Is the dst a real server address? If so, a flow needs to be inserted.
                // 2) Is the src a random address in use currently? If so, a flow needs to be inserted.
                if (randomizedServerList.containsKey(l3.getDestinationAddress())) {
                    log.info("3. Packet is destined for a server's real address contained in the randomized server list.");
                    insertDestinationEncryptFlow(sw, l3, OFPort.of(2));
                    return Command.STOP;
                } else if (randomizedServerList.containsValue(l3.getSourceAddress())) {
                    log.info("4. Packet is coming from a server's random address contained in the randomized server list.");
                    SwitchPort[] aps = getAttachmentPointsForDevice(l3.getDestinationAddress(), sw.getId());
                    if (aps != null) {
                        log.info("{}", aps);
                        insertSourceDecryptFlow(sw, l3, aps[0].getPortId()); // TODO Make ports dynamic
                    }
                    return Command.STOP;
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
        l.add(IDeviceService.class);
        l.add(IFloodlightProviderService.class);
        l.add(IOFSwitchService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        executorService = Executors.newSingleThreadScheduledExecutor();
        deviceService = context.getServiceImpl(IDeviceService.class);
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        staticEntryPusherService = context.getServiceImpl(IStaticEntryPusherService.class);
        log = LoggerFactory.getLogger(Randomizer.class);
        generator = new Random(SEED);

        Map<String, String> configParameters = context.getConfigParams(this);
        String tmp = configParameters.get("randomize-host");
        log.info("tmp is {}", tmp);
        if (tmp != null) {
            tmp = tmp.trim().toLowerCase();
            if (tmp.contains("yes") || tmp.contains("true")) {
                LOCAL_HOST_IS_RANDOMIZED = true;
                log.info("Local host will be treated as having randomized addresses.");
            } else {
                LOCAL_HOST_IS_RANDOMIZED = false;
                log.info("Local host will be treated as having a static address.");
            }
        }


        whiteListedHostsIPv4 = new ArrayList<>();
        whiteListedHostsIPv6 = new ArrayList<>();

        randomizedServerList = new HashMap<>();
        randomizedServerList.put(IPv4Address.of(10,0,0,4), IPv4Address.of(20,0,0,4));
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        switchService.addOFSwitchListener(this);
        startTest();

    }
    //endregion
    //================================================================================

    //================================================================================
    //region IOFSwitchListener Implementation
    @Override
    public void switchAdded(DatapathId switchId) {

    }

    @Override
    public void switchRemoved(DatapathId switchId) {

    }

    @Override
    public void switchActivated(DatapathId switchId) {

    }

    @Override
    public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {

    }

    @Override
    public void switchChanged(DatapathId switchId) {

    }

    @Override
    public void switchDeactivated(DatapathId switchId) {

    }
    //endregion
    //================================================================================


}
