package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.randomizer.web.RandomizerWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.quartz.DateBuilder.evenMinuteDateAfterNow;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

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

    private static List<Connection> connections;
    private static ServerManager serverManager;

    private static boolean enabled;
    private static boolean randomize;
    private static OFPort lanport;
    private static OFPort wanport;
    private static int addressUpdateInterval;
    private static int prefixUpdateInterval;

    //endregion
    //================================================================================


    //================================================================================
    //region Helper Functions

    private void scheduleJobs() {
        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
        Scheduler scheduler = null;
        try {
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }

        Trigger prefixtrigger = newTrigger()
                .withIdentity("trigger8") // because group is not specified, "trigger8" will be in the default group
                .startAt(evenMinuteDateAfterNow()) // get the next even-minute (seconds zero ("**:00"))
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(prefixUpdateInterval)
                        .repeatForever())
                // note that in this example, 'forJob(..)' is not called
                //  - which is valid if the trigger is passed to the scheduler along with the job
                .build();

        JobDetail prefixjob = JobBuilder.newJob(PrefixUpdateJob.class)
                .withIdentity("Prefix Update")
                .build();

        Trigger addresstrigger = newTrigger()
                .withIdentity("trigger9") // because group is not specified, "trigger8" will be in the default group
                .startAt(evenMinuteDateAfterNow()) // get the next even-minute (seconds zero ("**:00"))
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(addressUpdateInterval)
                        .repeatForever())
                // note that in this example, 'forJob(..)' is not called
                //  - which is valid if the trigger is passed to the scheduler along with the job
                .build();

        JobDetail addressjob = JobBuilder.newJob(AddressUpdateJob.class)
                .withIdentity("Address Update")
                .build();

        try {
            if (scheduler != null) {
                scheduler.scheduleJob(prefixjob, prefixtrigger);
                scheduler.scheduleJob(addressjob, addresstrigger);
                scheduler.start();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public static class AddressUpdateJob implements Job {
        Logger log = LoggerFactory.getLogger(AddressUpdateJob.class);

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            log.debug("Updating IP addresses for each server. Flows will be updated as well.");
            serverManager.updateServers();
            connections.forEach(Connection::update);
        }
    }

    public static class PrefixUpdateJob implements Job {
        Logger log = LoggerFactory.getLogger(PrefixUpdateJob.class);

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            log.debug("Updating prefixes for each server.");
            serverManager.getServers().forEach(Server::updatePrefix);
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
        FlowFactory.setRandomize(randomize);
        log.warn("Set randomize to {}", random);
        return RandomizerReturnCode.CONFIG_SET;
    }

    @Override
    public OFPort getLanPort() {
        return lanport;
    }

    @Override
    public RandomizerReturnCode setLanPort(int portnumber) {
        lanport = OFPort.of(portnumber);
        FlowFactory.setLanport(lanport);
        log.warn("Set lanport to {}", portnumber);
        return RandomizerReturnCode.CONFIG_SET;
    }

    @Override
    public OFPort getWanPort() {
        return wanport;
    }

    @Override
    public RandomizerReturnCode setWanPort(int portnumber) {
        wanport = OFPort.of(portnumber);
        FlowFactory.setWanport(wanport);
        log.warn("Set wanport to {}", portnumber);
        return RandomizerReturnCode.CONFIG_SET;
    }
    
    @Override
    public Server getServer(IPv4Address serveraddress) {
        return serverManager.getServer(serveraddress);
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

    @Override
    public Map<IPv4Address, IPv4AddressWithMask> getCurrentPrefix() {
        return serverManager.getServers().stream()
                .collect(Collectors.toMap(Server::getiPv4AddressReal, Server::getPrefix));
    }
    
    public Map<IPv4Address, List<IPv4AddressWithMask>> getPrefixes() {
        return serverManager.getServers().stream()
                .collect(Collectors.toMap(Server::getiPv4AddressReal, Server::getPrefixes));
    }

    @Override
    public void addPrefix(Server server, IPv4AddressWithMask prefix) {
        if (!serverManager.getServer(server.getiPv4AddressReal()).getPrefixes().contains(prefix)) {
            // TODO: This can be simplified a ton.
            serverManager.getServer(server.getiPv4AddressReal()).addPrefix(prefix);
        }
    }

    @Override
    public void removePrefix(Server server, IPv4AddressWithMask prefix) {
        if (serverManager.getServer(server.getiPv4AddressReal()).getPrefixes().contains(prefix)) {
            // TODO: This can also be simplified a lot.
            serverManager.getServer(server.getiPv4AddressReal()).removePrefix(prefix);
        }
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
        Server server;
        if (l2.getEtherType() == EthType.IPv4) {
            IPv4 l3 = (IPv4) l2.getPayload();
            
            /* Packet is coming from client to the servers fake IP on the randomized side*/
            if ((server = serverManager.getServerThatContainsIP(l3.getDestinationAddress())) != null) {
                log.debug("IPv4 packet destined for a randomized server's external prefix found: {}", server);
            }
            /* Packet is coming from the non-randomized client side (probably initiation) */
            else if ((server = serverManager.getServer(l3.getDestinationAddress())) != null) {
                log.debug("IPv4 packet destined for a randomized server's internal IP found: {}", server);
            }
            /* Packet is unrelated to any randomized server connection */
            else {
                log.debug("IPv4 packet address {} is not destined for a randomized server. Continuing...", l3.getDestinationAddress());
                return Command.CONTINUE;
            }

            for (Connection c : connections) {
                if (c.getServer().equals(server)) {
                    log.error("ERROR! Received packet that belongs to an existing connection...");
                    return Command.STOP;
                }
            }
            log.info("New EAGER connection created...");
            connections.add(new Connection(server, sw.getId()));
            return Command.STOP;
        } else if (l2.getEtherType() == EthType.ARP) {
            ARP arp = (ARP) l2.getPayload();
            
            if ((server = serverManager.getServerFromFakeIP(arp.getTargetProtocolAddress())) != null) {
                log.debug("ARP packet destined for a randomized server's external prefix found: {}", server);
            }
            else if((server = serverManager.getServer(arp.getTargetProtocolAddress())) != null) {
                log.debug("ARP packet destined for a randomized server's internal IP found: {}", server);
            }
            else {
                log.trace("ARP packet address {} is not destined for a randomized server. Continuing...", arp.getTargetProtocolAddress());
                return Command.CONTINUE;
            }

            for (Connection c : connections) {
                if (c.getServer().equals(server)) {
                    log.error("ERROR! Received packet that belongs to an existing connection...");
                    return Command.STOP;
                }
            }
            log.info("New EAGER connection created...");
            connections.add(new Connection(server, sw.getId()));
        } else {
            log.debug("Packet isn't ARP or IPv4. Continuing...");
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
//        ((ch.qos.logback.classic.Logger) log).setLevel(Level.DEBUG);
//        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Forwarding.class)).setLevel(Level.ERROR);
//        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LinkDiscoveryManager.class)).setLevel(Level.ERROR);

        connections = new ArrayList<Connection>();
        serverManager = new ServerManager();

        /* Add prefixes here */
        //prefixes.add(IPv4AddressWithMask.of("184.164.243.0/24"));

        /* Add servers here */
        //serverManager.addServer(new Server(IPv4Address.of(10, 0, 0, 1)));
        //serverManager.addServer(new Server(IPv4Address.of(20, 0, 0, 1)));
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
            lanport = OFPort.of(Integer.parseInt(configOptions.get("lanport")));
            wanport = OFPort.of(Integer.parseInt(configOptions.get("wanport")));
            addressUpdateInterval = Integer.parseInt(configOptions.get("addressUpdateIntervalInSeconds"));
            prefixUpdateInterval = Integer.parseInt(configOptions.get("prefixUpdateIntervalInSeconds"));
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.error("Incorrect Randomizer configuration options. Required: 'enabled', 'randomize', 'lanport', 'wanport'", ex);
            throw ex;
        }

        if (log.isInfoEnabled()) {
            log.info("Initial config options: enabled:{}, randomize:{}, lanport:{}, wanport:{}",
                    new Object[]{enabled, randomize, lanport, wanport});
        }

        FlowFactory.setRandomize(randomize);
        FlowFactory.setWanport(wanport);
        FlowFactory.setLanport(lanport);

        //updatePrefixes();
        //updateIPs();
        scheduleJobs();
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
