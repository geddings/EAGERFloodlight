package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;

/**
 * Created by geddingsbarrineau on 9/19/16.
 */
public interface IRandomizerService extends IFloodlightService {

    /**
     * Check if the Randomizer module is enabled.
     * @return True if enabled
     */
    public boolean isEnabled();

    /**
     * Enable Randomizer module
     * @return enabled
     */
    public RandomizerReturnCode enable();

    /**
     * Disable Randomizer module
     * @return disabled
     */
    public RandomizerReturnCode disable();

    /**
     * Check if Floodlight is randomizing hosts.
     * @return True if hosts are randomized, else False
     */
    public boolean isRandom();

    public RandomizerReturnCode setRandom(Boolean random);

    /**
     * Retrieve the configured local port
     * @return localport
     */
    public OFPort getLocalPort();

    public RandomizerReturnCode setLocalPort(int portnumber);

    /**
     * Retrieve the configured wan port
     * @return wanport
     */
    public OFPort getWanPort();

    public RandomizerReturnCode setWanPort(int portnumber);

    public List<Server> getServers();


    public enum RandomizerReturnCode {
        WHITELIST_ENTRY_ADDED, WHITELIST_ENTRY_REMOVED,
        ERR_DUPLICATE_WHITELIST_ENTRY, ERR_UNKNOWN_WHITELIST_ENTRY,
        AGENT_ADDED, AGENT_REMOVED,
        ERR_DUPLICATE_AGENT, ERR_UNKNOWN_AGENT,
        ENABLED, DISABLED,
        CONFIG_SET,
        READY, NOT_READY,
        STATS_CLEARED
    }

}
