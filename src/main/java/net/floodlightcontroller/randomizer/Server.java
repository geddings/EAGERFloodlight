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
    private IPv4Address externalIP;

    private List<IPv4AddressWithMask> prefixes;
    private IPv4AddressWithMask prefix;
    private Random generator;

    public Server(IPv4Address internalIP, List<IPv4AddressWithMask> prefixes) {
        super(internalIP, true);
        this.prefixes = prefixes;
        generator = new Random();
        updatePrefix();
        update();
    }

    public Server(IPv4Address iPv4AddressReal) {
        super(iPv4AddressReal, true);
        prefixes = new ArrayList<>();
        generator = new Random();
        updatePrefix();
        update();
    }

    public void update() {
        generator.setSeed(LocalTime.now().toSecondOfDay() % getInternalIP().getInt());
        externalIP = IPv4Address.of(generator.nextInt())
                .and(prefix.getMask().not())
                .or(prefix.getValue());
        log.debug("New external address: {}", externalIP);
    }

    public void updatePrefix() {
        if (!prefixes.isEmpty()) {
            prefix = prefixes.get(LocalDateTime.now().getMinute() % prefixes.size());
        } else {
            prefix = IPv4AddressWithMask.NONE;
        }
    }

    public IPv4Address getExternalIP() {
        return externalIP;
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
                "externalIP=" + externalIP +
                ", prefix=" + prefix +
                "} " + super.toString();
    }
}
