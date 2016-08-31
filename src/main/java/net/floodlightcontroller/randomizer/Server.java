package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.devicemanager.SwitchPort;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;

/**
 * Created by geddingsbarrineau on 8/31/16.
 */
public class Server {

    private IPv4Address iPv4AddressReal;
    private IPv4Address iPv4AddressFake;

    private IPv6Address iPv6AddressReal;
    private IPv6Address iPv6AddressFake;

    private SwitchPort switchPort;

    public Server(IPv4Address iPv4AddressReal, IPv4Address iPv4AddressFake, IPv6Address iPv6AddressReal, IPv6Address iPv6AddressFake, SwitchPort switchPort) {
        this.iPv4AddressReal = iPv4AddressReal;
        this.iPv4AddressFake = iPv4AddressFake;
        this.iPv6AddressReal = iPv6AddressReal;
        this.iPv6AddressFake = iPv6AddressFake;
        this.switchPort = switchPort;
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

    public void setiPv4AddressFake(IPv4Address iPv4AddressFake) {
        this.iPv4AddressFake = iPv4AddressFake;
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

    public void setiPv6AddressFake(IPv6Address iPv6AddressFake) {
        this.iPv6AddressFake = iPv6AddressFake;
    }

    public SwitchPort getSwitchPort() {
        return switchPort;
    }

    public void setSwitchPort(SwitchPort switchPort) {
        this.switchPort = switchPort;
    }

    @Override
    public String toString() {
        return "Server{" +
                "iPv4AddressReal=" + iPv4AddressReal +
                ", iPv4AddressFake=" + iPv4AddressFake +
                ", iPv6AddressReal=" + iPv6AddressReal +
                ", iPv6AddressFake=" + iPv6AddressFake +
                ", switchPort=" + switchPort +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Server server = (Server) o;

        if (iPv4AddressReal != null ? !iPv4AddressReal.equals(server.iPv4AddressReal) : server.iPv4AddressReal != null)
            return false;
        if (iPv4AddressFake != null ? !iPv4AddressFake.equals(server.iPv4AddressFake) : server.iPv4AddressFake != null)
            return false;
        if (iPv6AddressReal != null ? !iPv6AddressReal.equals(server.iPv6AddressReal) : server.iPv6AddressReal != null)
            return false;
        if (iPv6AddressFake != null ? !iPv6AddressFake.equals(server.iPv6AddressFake) : server.iPv6AddressFake != null)
            return false;
        return switchPort != null ? switchPort.equals(server.switchPort) : server.switchPort == null;

    }

    @Override
    public int hashCode() {
        int result = iPv4AddressReal != null ? iPv4AddressReal.hashCode() : 0;
        result = 31 * result + (iPv4AddressFake != null ? iPv4AddressFake.hashCode() : 0);
        result = 31 * result + (iPv6AddressReal != null ? iPv6AddressReal.hashCode() : 0);
        result = 31 * result + (iPv6AddressFake != null ? iPv6AddressFake.hashCode() : 0);
        result = 31 * result + (switchPort != null ? switchPort.hashCode() : 0);
        return result;
    }

    public static class ServerBuilder {

        private IPv4Address iPv4AddressReal = null;
        private IPv4Address iPv4AddressFake = null;
        private IPv6Address iPv6AddressReal = null;
        private IPv6Address iPv6AddressFake = null;
        private SwitchPort switchPort = null;

        public ServerBuilder setiPv4AddressReal(IPv4Address iPv4AddressReal) {
            this.iPv4AddressReal = iPv4AddressReal;
            return this;
        }

        public ServerBuilder setiPv4AddressFake(IPv4Address iPv4AddressFake) {
            this.iPv4AddressFake = iPv4AddressFake;
            return this;
        }

        public ServerBuilder setiPv6AddressReal(IPv6Address iPv6AddressReal) {
            this.iPv6AddressReal = iPv6AddressReal;
            return this;
        }

        public ServerBuilder setiPv6AddressFake(IPv6Address iPv6AddressFake) {
            this.iPv6AddressFake = iPv6AddressFake;
            return this;
        }

        public ServerBuilder setSwitchPort(SwitchPort switchPort) {
            this.switchPort = switchPort;
            return this;
        }

        public Server createServer() {
            return new Server(iPv4AddressReal, iPv4AddressFake, iPv6AddressReal, iPv6AddressFake, switchPort);
        }
    }
}
