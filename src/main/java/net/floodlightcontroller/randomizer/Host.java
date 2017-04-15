package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.IPv4Address;

/**
 * Created by geddingsbarrineau on 4/7/17.
 */
public class Host {

    private IPv4Address address;

    public Host(IPv4Address address) {
        this.address = address;
    }

    public IPv4Address getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Host{" +
                "address=" + address +
                '}';
    }
}
