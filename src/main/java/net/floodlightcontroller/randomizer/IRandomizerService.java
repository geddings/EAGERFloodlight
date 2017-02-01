package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;

/**
 * Created by geddingsbarrineau on 9/19/16.
 *
 */
public interface IRandomizerService extends IFloodlightService {

    /**
     * Check if the Randomizer module is enabled.
     *
     * @return True if enabled
     */
    boolean isEnabled();

    /**
     * Enable Randomizer module
     *
     * @return enabled
     */
    RandomizerReturnCode enable();

    /**
     * Disable Randomizer module
     *
     * @return disabled
     */
    RandomizerReturnCode disable();

    /**
     * Check if Floodlight is randomizing hosts.
     *
     * @return True if hosts are randomized, else False
     */
    boolean isRandom();

    RandomizerReturnCode setRandom(Boolean random);

    /**
     * Retrieve the configured local port
     *
     * @return localport
     */
    OFPort getLanPort();

    RandomizerReturnCode setLanPort(int portnumber);

    /**
     * Retrieve the configured wan port
     *
     * @return wanport
     */
    OFPort getWanPort();

    RandomizerReturnCode setWanPort(int portnumber);

    List<Server> getServers();

    RandomizerReturnCode addServer(Server server);

    RandomizerReturnCode removeServer(Server server);

    List<Connection> getConnections();

    RandomizerReturnCode addConnection(Connection connection);

    RandomizerReturnCode removeConnection(Connection connection);

    IPv4AddressWithMask getCurrentPrefix();
    
    List<IPv4AddressWithMask> getPrefixes();
    
    void addPrefix(IPv4AddressWithMask prefix);
    
    void removePrefix(IPv4AddressWithMask prefix);

    enum RandomizerReturnCode {
        WHITELIST_ENTRY_ADDED, WHITELIST_ENTRY_REMOVED,
        ERR_DUPLICATE_WHITELIST_ENTRY, ERR_UNKNOWN_WHITELIST_ENTRY,
        SERVER_ADDED, SERVER_REMOVED,
        ERR_DUPLICATE_SERVER, ERR_UNKNOWN_SERVER,
        CONNECTION_ADDED, CONNECTION_REMOVED,
        ERR_DUPLICATE_CONNECTION, ERR_UNKNOWN_CONNECTION,
        ENABLED, DISABLED,
        CONFIG_SET,
        READY, NOT_READY,
        STATS_CLEARED
    }

}
