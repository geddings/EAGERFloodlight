package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.devicemanager.SwitchPort;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;

import java.util.Random;

/**
 * Created by geddingsbarrineau on 8/31/16.
 */
public class Server {

    private IPv4Address iPv4AddressReal;
    private IPv4Address iPv4AddressFakePrev;
    private IPv4Address iPv4AddressFake;
    private IPv4Address iPv4AddressFakeNext;
    private IPv6Address iPv6AddressReal;
    private IPv6Address iPv6AddressFakePrev;
    private IPv6Address iPv6AddressFake;
    private IPv6Address iPv6AddressFakeNext;
    private SwitchPort switchPort;

    private Random generator;
    private int seed;

    public Server(IPv4Address iPv4AddressReal, int randomSeed) {
        this.iPv4AddressReal = iPv4AddressReal;
        this.seed = randomSeed;

        generator = new Random(seed);
        /* TODO Figure out a way to dynamically apply a new prefix. Also determine who should own it. */
        this.iPv4AddressFake = IPv4Address.of(generator.nextInt())
                .and(IPv4Address.of(0,0,0,255))
                .or(IPv4Address.of(10,0,0,0));
        this.iPv4AddressFakeNext = IPv4Address.of(generator.nextInt())
                .and(IPv4Address.of(0,0,0,255))
                .or(IPv4Address.of(10,0,0,0));
    }

    public void update() {
        iPv4AddressFakePrev = iPv4AddressFake;
        iPv4AddressFake = iPv4AddressFakeNext;
        iPv4AddressFakeNext = IPv4Address.of(generator.nextInt())
                .and(IPv4Address.of(0,0,0,255))
                .or(IPv4Address.of(10,0,0,0));
    }

    @Override
    public String toString() {
        return "Server{" +
                "iPv4AddressReal=" + iPv4AddressReal +
                ", iPv4AddressFakePrev=" + iPv4AddressFakePrev +
                ", iPv4AddressFake=" + iPv4AddressFake +
                ", iPv4AddressFakeNext=" + iPv4AddressFakeNext +
                ", iPv6AddressReal=" + iPv6AddressReal +
                ", iPv6AddressFakePrev=" + iPv6AddressFakePrev +
                ", iPv6AddressFake=" + iPv6AddressFake +
                ", iPv6AddressFakeNext=" + iPv6AddressFakeNext +
                ", switchPort=" + switchPort +
                '}';
    }

    public IPv6Address getiPv6AddressFakePrev() {
        return iPv6AddressFakePrev;
    }

    public IPv6Address getiPv6AddressFakeNext() {
        return iPv6AddressFakeNext;
    }

    public IPv4Address getiPv4AddressFakePrev() {
        return iPv4AddressFakePrev;
    }

    public IPv4Address getiPv4AddressFakeNext() {
        return iPv4AddressFakeNext;
    }

    public IPv4Address getiPv4AddressReal() {
        return iPv4AddressReal;
    }

    public void setiPv4AddressReal(IPv4Address iPv4AddressReal) {
        this.iPv4AddressReal = iPv4AddressReal;
    }

    public IPv4Address getiPv4AddressFake() {
        return iPv4AddressFake;
    }

    public IPv6Address getiPv6AddressReal() {
        return iPv6AddressReal;
    }

    public void setiPv6AddressReal(IPv6Address iPv6AddressReal) {
        this.iPv6AddressReal = iPv6AddressReal;
    }

    public IPv6Address getiPv6AddressFake() {
        return iPv6AddressFake;
    }

    public SwitchPort getSwitchPort() {
        return switchPort;
    }

    public void setSwitchPort(SwitchPort switchPort) {
        this.switchPort = switchPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (seed != server.seed) return false;
        if (iPv4AddressReal != null ? !iPv4AddressReal.equals(server.iPv4AddressReal) : server.iPv4AddressReal != null)
            return false;
        return iPv6AddressReal != null ? iPv6AddressReal.equals(server.iPv6AddressReal) : server.iPv6AddressReal == null;

    }

    @Override
    public int hashCode() {
        int result = iPv4AddressReal != null ? iPv4AddressReal.hashCode() : 0;
        result = 31 * result + (iPv6AddressReal != null ? iPv6AddressReal.hashCode() : 0);
        result = 31 * result + seed;
        return result;
    }
}
