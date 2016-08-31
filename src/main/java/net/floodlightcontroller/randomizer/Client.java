package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.devicemanager.SwitchPort;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;

/**
 * Created by geddingsbarrineau on 8/31/16.
 */
public class Client {
    private IPv4Address ipv4address;
    private IPv6Address iPv6Address;
    private SwitchPort switchPort;

    public Client(IPv4Address ipv4address, IPv6Address iPv6Address, SwitchPort switchPort) {
        this.ipv4address = ipv4address;
        this.iPv6Address = iPv6Address;
        this.switchPort = switchPort;
    }

    public IPv4Address getIpv4address() {
        return ipv4address;
    }

    public void setIpv4address(IPv4Address ipv4address) {
        this.ipv4address = ipv4address;
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
                "ipv4address=" + ipv4address +
                ", iPv6Address=" + iPv6Address +
                ", switchPort=" + switchPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;

        if (ipv4address != null ? !ipv4address.equals(client.ipv4address) : client.ipv4address != null) return false;
        if (iPv6Address != null ? !iPv6Address.equals(client.iPv6Address) : client.iPv6Address != null) return false;
        return switchPort != null ? switchPort.equals(client.switchPort) : client.switchPort == null;

    }

    @Override
    public int hashCode() {
        int result = ipv4address != null ? ipv4address.hashCode() : 0;
        result = 31 * result + (iPv6Address != null ? iPv6Address.hashCode() : 0);
        result = 31 * result + (switchPort != null ? switchPort.hashCode() : 0);
        return result;
    }

}
