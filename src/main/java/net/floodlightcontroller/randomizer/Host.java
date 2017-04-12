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

    public IPv4Address getAddressForMatch(Connection.Direction direction) {
        return address;
    }
    
    public IPv4Address getAddressForAction(Connection.Direction direction) {
        return null;
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        return address != null ? address.equals(host.address) : host.address == null;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Host{" +
                "address=" + address +
                '}';
    }
}
