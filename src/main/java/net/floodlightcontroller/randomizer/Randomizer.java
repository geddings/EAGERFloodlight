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
import net.floodlightcontroller.randomizer.web.RandomizerWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by geddingsbarrineau on 7/14/16.
 */
public class Randomizer implements IOFMessageListener, IOFSwitchListener, IFloodlightModule, IRandomizerService {

    //================================================================================
    //region Properties
    private ScheduledExecutorService executorService;
    private IDeviceService deviceService;
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;
    protected static IOFSwitchService switchService;
    private static Logger log;

    private List<Connection> connections;
    private ServerManager serverManager;

    private static boolean enabled;
    private static boolean randomize;
    private static OFPort localport;
    private static OFPort wanport;

    //endregion
    //================================================================================


    //================================================================================
    //region Helper Functions

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

    private void startTest() {
        executorService.scheduleAtFixedRate(() -> {
           serverManager.updateServers();
        }, 0L, 30L, TimeUnit.SECONDS);
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
    //region IRandomizerService Implementation

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public RandomizerReturnCode enable() {
        log.warn("Enabling Randomizer");
        enabled = true;
        return RandomizerReturnCode.ENABLED;
    }

    @Override
    public RandomizerReturnCode disable() {
        log.warn("Disabling Randomizer");
        enabled = false;
        return RandomizerReturnCode.DISABLED;
    }

    @Override
    public boolean isRandom() {
        return randomize;
    }

    @Override
    public RandomizerReturnCode setRandom(Boolean random) {
        randomize = random;
        log.warn("Set randomize to {}", random);
        return RandomizerReturnCode.CONFIG_SET;
    }

    @Override
    public OFPort getLocalPort() {
        return localport;
    }

    @Override
    public RandomizerReturnCode setLocalPort(int portnumber) {
        localport = OFPort.of(portnumber);
        log.warn("Set localport to {}", portnumber);
        return RandomizerReturnCode.CONFIG_SET;
    }

    @Override
    public OFPort getWanPort() {
        return wanport;
    }

    @Override
    public RandomizerReturnCode setWanPort(int portnumber) {
        wanport = OFPort.of(portnumber);
        log.warn("Set wanport to {}", portnumber);
        return RandomizerReturnCode.CONFIG_SET;
    }

    @Override
    public List<Server> getServers() {
        return serverManager.getServers();
    }

    //endregion
    //================================================================================

    //================================================================================
    //region IOFMessageListener Implementation
    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        /*
		 * If we're disabled, then just stop now
		 * and let Forwarding/Hub handle the connection.
		 */
        if (!enabled) {
            log.trace("Randomizer disabled. Not acting on packet; passing to next module.");
            return Command.CONTINUE;
        } else {
			/*
			 * Randomizer is enabled; proceed
			 */
            log.trace("Randomizer enabled. Inspecting packet to see if it's a candidate for randomization.");
        }
        OFPacketIn pi = (OFPacketIn) msg;
        OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
        Ethernet l2 = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        if (l2.getEtherType() == EthType.IPv4) {
            IPv4 l3 = (IPv4) l2.getPayload();

            Server server;
            /* Packet is coming from client to the servers fake IP on the randomized side*/
            if ((server = serverManager.getServerFake(l3.getDestinationAddress())) != null) {
                log.info("Packet destined for a randomized server's fake IP found...");
            }
            /* Packet is coming from the non-randomized client side (probably initiation) */
            else if ((server = serverManager.getServer(l3.getDestinationAddress())) != null) {
                log.info("Packet destined for a randomized server's real IP found...");
            }
            /* Packet is unrelated to any randomized server connection */
            else {
                log.info("Neither source nor destination IPv4 addresses matches a server. Continuing...");
                return Command.CONTINUE;
            }

            for (Connection c : connections) {
                if (c.getServer().equals(server)) {
                    log.info("ERROR! Received packet that belongs to an existing connection...");
                    return Command.STOP;
                }
            }
            connections.add(new Connection(server, sw.getId(), wanport, localport, randomize));
            return Command.STOP;

            //region Old receive implementation
            /*
            // Is the local host supposed to be randomized? (Defined in the properties file)
            if (LOCAL_HOST_IS_RANDOMIZED) {
                log.info("IPv4 packet seen on Randomized host. \nSrc:{}\nDst:{}\nInPort:{}", new Object[] {l3.getSourceAddress(), l3.getDestinationAddress(),
                        inPort});
                // Is this packet part of the randomized connection?
                // For randomized host, there are two things to look for:
                // 1) Is the dst a randomize address in use currently? If so, a flow needs to be inserted.
                // 2) Is the src a real server address? If so, a flow needs to be inserted.
                if (randomizedServerList.containsValue(l3.getDestinationAddress())) {
                    log.info("1. Packet is destined for a server's randomize address contained in the randomized server list.");
                    SwitchPort[] aps = getAttachmentPointsForDevice((IPv4Address)getKeyFromValue(
                            randomizedServerList, l3.getDestinationAddress()), sw.getId());
                    if (aps != null) {
                        log.info("{}", aps);
                        insertDestinationDecryptFlow(sw, l3, aps[0].getPortId()); // TODO Make ports dynamic
                    }
                    return Command.STOP;
                } else if (randomizedServerList.containsKey(l3.getSourceAddress())) {
                    log.info("2. Packet is coming from a server's real address contained in the randomized server list.");
                    insertSourceEncryptFlow(sw, l3, OFPort.of(2)); // This port should be the port that leads to the BGP router.
                    return Command.STOP;
                }
            } else {
                log.info("IPv4 packet seen on non-Randomized host. \nSrc:{}\nDst:{}\nInPort:{}", new Object[] {l3.getSourceAddress(), l3.getDestinationAddress(),
                        inPort});
                // Is this packet part of the randomized connection?
                // For non-randomized host, there are two things to look for:
                // 1) Is the dst a real server address? If so, a flow needs to be inserted.
                // 2) Is the src a randomize address in use currently? If so, a flow needs to be inserted.
                if (randomizedServerList.containsKey(l3.getDestinationAddress())) {
                    log.info("3. Packet is destined for a server's real address contained in the randomized server list.");
                    insertDestinationEncryptFlow(sw, l3, OFPort.of(2)); // This port should be the port that leads to the BGP router.
                    return Command.STOP;
                } else if (randomizedServerList.containsValue(l3.getSourceAddress())) {
                    log.info("4. Packet is coming from a server's randomize address contained in the randomized server list.");
                    SwitchPort[] aps = getAttachmentPointsForDevice(l3.getDestinationAddress(), sw.getId());
                    if (aps != null) {
                        log.info("{}", aps);
                        insertSourceDecryptFlow(sw, l3, aps[0].getPortId()); // TODO Make ports dynamic
                    }
                    return Command.STOP;
                }
            } */
            //endregion
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
        Collection<Class<? extends IFloodlightService>> s = new HashSet<>();
        s.add(IRandomizerService.class);
        return s;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<>();
        m.put(IRandomizerService.class, this);
        return m;
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
        restApiService = context.getServiceImpl(IRestApiService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        log = LoggerFactory.getLogger(Randomizer.class);

        connections = new ArrayList<Connection>();
        serverManager = new ServerManager();

        /* Add servers here */
        serverManager.addServer(new Server(IPv4Address.of(10,0,0,4), 1234));
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        switchService.addOFSwitchListener(this);
        restApiService.addRestletRoutable(new RandomizerWebRoutable());

        Map<String, String> configOptions = context.getConfigParams(this);
        try {
			/* These are defaults */
			enabled = Boolean.parseBoolean(configOptions.get("enabled"));
			randomize = Boolean.parseBoolean(configOptions.get("randomize"));
			localport = OFPort.of(Integer.parseInt(configOptions.get("localport")));
            wanport = OFPort.of(Integer.parseInt(configOptions.get("wanport")));

        } catch (IllegalArgumentException | NullPointerException ex) {
            log.error("Incorrect Randomizer configuration options. Required: 'enabled', 'randomize', 'localport', 'wanport'", ex);
            throw ex;
        }

        if (log.isInfoEnabled()) {
            log.info("Initial config options: enabled:{}, randomize:{}, localport:{}, wanport:{}",
                    new Object[] { enabled, randomize, localport, wanport });
        }

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
