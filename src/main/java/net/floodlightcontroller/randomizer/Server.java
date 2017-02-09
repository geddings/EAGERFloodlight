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
 *
 * This is a Server object for the EAGER project.
 */
@JsonSerialize(using = ServerSerializer.class)
public class Server {
    private static Logger log = LoggerFactory.getLogger(Server.class);
    private IPv4Address iPv4AddressReal;
    private IPv4Address iPv4AddressFake;

    private List<IPv4AddressWithMask> prefixes;
    private IPv4AddressWithMask prefix;
    private Random generator;

    public Server(IPv4Address iPv4AddressReal, List<IPv4AddressWithMask> prefixes) {
        this.iPv4AddressReal = iPv4AddressReal;
        this.prefixes = prefixes;
        generator = new Random();
        updatePrefix();
        update();
    }
    
    public Server(IPv4Address iPv4AddressReal) {
        this.iPv4AddressReal = iPv4AddressReal;
        prefixes = new ArrayList<>();
        generator = new Random();
        updatePrefix();
        update();
    }

    public void update() {
        generator.setSeed(LocalTime.now().toSecondOfDay() % iPv4AddressReal.getInt());
        iPv4AddressFake = IPv4Address.of(generator.nextInt())
                .and(prefix.getMask().not())
                .or(prefix.getValue());
        log.debug("New fake address: {}", iPv4AddressFake);
    }
    
    public void updatePrefix() {
        if (!prefixes.isEmpty()) {
            prefix = prefixes.get(LocalDateTime.now().getMinute() % prefixes.size());
        } else {
            prefix = IPv4AddressWithMask.NONE;
        }
    }

    public IPv4Address getiPv4AddressReal() {
        return iPv4AddressReal;
    }

    public IPv4Address getiPv4AddressFake() {
        return iPv4AddressFake;
    }

    public IPv4AddressWithMask getPrefix() {
        return prefix;
    }

//    public void setPrefix(IPv4AddressWithMask prefix) {
//        this.prefix = prefix;
//        log.debug("New prefix: {}", prefix);
//    }
    
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        return iPv4AddressReal != null ? iPv4AddressReal.equals(server.iPv4AddressReal) : server.iPv4AddressReal == null;

    }

    @Override
    public int hashCode() {
        return iPv4AddressReal != null ? iPv4AddressReal.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Server{" +
                "iPv4AddressReal=" + iPv4AddressReal +
                ", iPv4AddressFake=" + iPv4AddressFake +
                ", prefix=" + prefix +
                '}';
    }
}
