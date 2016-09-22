package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Created by geddingsbarrineau on 8/31/16.
 *
 * This is a Server object for the EAGER project.
 */
public class Server {
    private static Logger log = LoggerFactory.getLogger(Server.class);
    private IPv4Address iPv4AddressReal;
    private IPv4Address iPv4AddressFake;

    private IPv4AddressWithMask prefix;
    private Random generator;

    public Server(IPv4Address iPv4AddressReal, IPv4AddressWithMask prefix) {
        this.iPv4AddressReal = iPv4AddressReal;
        this.prefix = prefix;
        generator = new Random();
        update();
    }

    public void update() {
        iPv4AddressFake = IPv4Address.of(generator.nextInt())
                .and(prefix.getMask().not())
                .or(prefix.getValue());
        log.info("New fake address: {}", iPv4AddressFake);
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

    public void setPrefix(IPv4AddressWithMask prefix) {
        this.prefix = prefix;
        log.info("New prefix: {}", prefix);
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
