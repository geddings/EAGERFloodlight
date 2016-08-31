package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.devicemanager.SwitchPort;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;

/**
 * Created by geddingsbarrineau on 8/31/16.
 */
public class Client {
    private IPv4Address iPv4Address;
    private IPv6Address iPv6Address;
    private SwitchPort switchPort;

    public Client(IPv4Address iPv4address, IPv6Address iPv6Address, SwitchPort switchPort) {
        this.iPv4Address = iPv4address;
        this.iPv6Address = iPv6Address;
        this.switchPort = switchPort;
    }

    public IPv4Address getIpv4address() {
        return iPv4Address;
    }

    public void setIpv4address(IPv4Address iPv4Address) {
        this.iPv4Address = iPv4Address;
    }

    public IPv6Address getiPv6Address() {
        return iPv6Address;
    }

    public void setiPv6Address(IPv6Address iPv6Address) {
        this.iPv6Address = iPv6Address;
    }

    public SwitchPort getSwitchPort() {
        return switchPort;
    }

    public void setSwitchPort(SwitchPort switchPort) {
        this.switchPort = switchPort;
    }

    @Override
    public String toString() {
        return "Client{" +
                "iPv4Address=" + iPv4Address +
                ", iPv6Address=" + iPv6Address +
                ", switchPort=" + switchPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;

        if (iPv4Address != null ? !iPv4Address.equals(client.iPv4Address) : client.iPv4Address != null) return false;
        if (iPv6Address != null ? !iPv6Address.equals(client.iPv6Address) : client.iPv6Address != null) return false;
        return switchPort != null ? switchPort.equals(client.switchPort) : client.switchPort == null;

    }

    @Override
    public int hashCode() {
        int result = iPv4Address != null ? iPv4Address.hashCode() : 0;
        result = 31 * result + (iPv6Address != null ? iPv6Address.hashCode() : 0);
        result = 31 * result + (switchPort != null ? switchPort.hashCode() : 0);
        return result;
    }

}
