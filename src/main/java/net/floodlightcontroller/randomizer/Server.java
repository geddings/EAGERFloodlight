package net.floodlightcontroller.randomizer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.randomizer.web.ServerSerializer;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by geddingsbarrineau on 8/31/16.
 * <p>
 * This is a Server object for the EAGER project.
 */
@JsonSerialize(using = ServerSerializer.class)
public class Server extends Host {
    private static Logger log = LoggerFactory.getLogger(Server.class);
    private IPv4Address randomizedAddress;

    private List<IPv4AddressWithMask> prefixes;
    private IPv4AddressWithMask prefix;
    private Random generator;

    public Server(IPv4Address address, List<IPv4AddressWithMask> prefixes) {
        super(address);
        this.prefixes = prefixes;
        generator = new Random();
        updatePrefix();
        update();
    }

    public Server(IPv4Address address) {
        super(address);
        prefixes = new ArrayList<>();
        generator = new Random();
        updatePrefix();
        update();
    }

    public void update() {
        generator.setSeed(LocalTime.now().toSecondOfDay() % getAddress().getInt());
        randomizedAddress = IPv4Address.of(generator.nextInt())
                .and(prefix.getMask().not())
                .or(prefix.getValue());
        log.debug("New external address: {}", randomizedAddress);
    }

    public void updatePrefix() {
        if (!prefixes.isEmpty()) {
            prefix = prefixes.get(LocalDateTime.now().getMinute() % prefixes.size());
        } else {
            prefix = IPv4AddressWithMask.NONE;
        }
    }

    public IPv4Address getRandomizedAddress() {
        return randomizedAddress;
    }
    
    @Override
    public IPv4Address getAddressForMatch(Connection.Direction direction) {
        return (direction == Connection.Direction.OUTGOING) ? getAddress() : getRandomizedAddress();
    }
    
    @Override
    public IPv4Address getAddressForAction(Connection.Direction direction) {
        return (direction == Connection.Direction.OUTGOING) ? getRandomizedAddress() : getAddress();
    }

    public IPv4AddressWithMask getPrefix() {
        return prefix;
    }

    public void addPrefix(IPv4AddressWithMask prefix) {
        prefixes.add(prefix);
    }

    public void removePrefix(IPv4AddressWithMask prefix) {
        prefixes.remove(prefix);
    }

    public List<IPv4AddressWithMask> getPrefixes() {
        return prefixes;
    }
    
    @Override
    public String toString() {
        return "Server{" +
                "randomizedAddress=" + randomizedAddress +
                ", prefix=" + prefix +
                "} " + super.toString();
    }
}
