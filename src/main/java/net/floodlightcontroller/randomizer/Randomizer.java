package net.floodlightcontroller.randomizer;

import ch.qos.logback.classic.Level;
import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.forwarding.Forwarding;
import net.floodlightcontroller.linkdiscovery.internal.LinkDiscoveryManager;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.randomizer.web.RandomizerWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;
import org.projectfloodlight.openflow.protocol.*;
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
 * <p>
 * This is the Randomizer Floodlight module.
 */
public class Randomizer implements IOFMessageListener, IOFSwitchListener, IFloodlightModule, IRandomizerService {

    //================================================================================
    //region Properties
    private ScheduledExecutorService executorService;
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;
    protected static IOFSwitchService switchService;
    private static Logger log;

    private List<Connection> connections;
    private ServerManager serverManager;

    private List<IPv4AddressWithMask> prefixes;

    private static boolean enabled;
    private static boolean randomize;
    private static OFPort localport;
    private static OFPort wanport;

    //endregion
    //================================================================================


    //================================================================================
    //region Helper Functions

    private void updateIPs() {
        executorService.scheduleAtFixedRate(() -> {
            log.warn("Updating IP addresses for each server. Flows will be updated as well.");
            serverManager.updateServers();
            connections.forEach(Connection::update);
        }, 0L, 10L, TimeUnit.SECONDS);
    }

    private void updatePrefixes() {
        executorService.scheduleAtFixedRate(() -> {
            // FIXME: THIS IS ONLY TEMPORARY AND WILL NOT SCALE AT ALL
            log.warn("Updating prefixes for each server.");
            serverManager.getServers().forEach(server -> server
                    .setPrefix(prefixes.get(Calendar.MINUTE % prefixes.size())));
        }, 0L, 1L, TimeUnit.MINUTES);
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

    @Override
    public RandomizerReturnCode addServer(Server server) {
        // Todo Make this portion more robust by adding more checks as needed
        serverManager.addServer(server);
        return RandomizerReturnCode.SERVER_ADDED;
    }

    @Override
    public RandomizerReturnCode removeServer(Server server) {
        // Todo Make this portion more robust by adding more checks as needed
        serverManager.removeServer(server);
        return RandomizerReturnCode.SERVER_REMOVED;
    }

    @Override
    public List<Connection> getConnections() {
        return connections;
    }

    @Override
    public RandomizerReturnCode addConnection(Connection connection) {
        connections.add(connection);
        return RandomizerReturnCode.CONNECTION_ADDED;
    }

    @Override
    public RandomizerReturnCode removeConnection(Connection connection) {
        connections.remove(connection);
        return RandomizerReturnCode.CONNECTION_REMOVED;
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
            if ((server = serverManager.getServerThatContainsIP(l3.getDestinationAddress())) != null) {
                log.info("Packet destined for a randomized server's fake prefix found...");
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
                    log.error("ERROR! Received packet that belongs to an existing connection...");
                    return Command.STOP;
                }
            }
            log.info("New EAGER connection created...");
            connections.add(new Connection(server, sw.getId(), wanport, localport, randomize));
            return Command.STOP;
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
        executorService = Executors.newScheduledThreadPool(2);
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        log = LoggerFactory.getLogger(Randomizer.class);

        /* For testing only: Set log levels of other classes */
        ((ch.qos.logback.classic.Logger) log).setLevel(Level.DEBUG);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Forwarding.class)).setLevel(Level.ERROR);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LinkDiscoveryManager.class)).setLevel(Level.ERROR);

        connections = new ArrayList<Connection>();
        serverManager = new ServerManager();
        prefixes = new ArrayList<IPv4AddressWithMask>();

        /* Add prefixes here */
        prefixes.add(IPv4AddressWithMask.of("20.0.0.0/24"));
        prefixes.add(IPv4AddressWithMask.of("30.0.0.0/24"));
        prefixes.add(IPv4AddressWithMask.of("40.0.0.0/24"));

        /* Add servers here */
        serverManager.addServer(new Server(IPv4Address.of(10, 0, 0, 2), IPv4AddressWithMask.NONE));
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
                    new Object[]{enabled, randomize, localport, wanport});
        }

        updatePrefixes();
        updateIPs();
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
