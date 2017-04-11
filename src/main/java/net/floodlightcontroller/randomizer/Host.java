package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.IPv4Address;

/**
 * Created by geddingsbarrineau on 4/7/17.
 */
public class Host {
    
    private IPv4Address internalIP;
    
    private boolean randomized;
    
    public Host(IPv4Address internalIP, Boolean randomized) {
        this.internalIP = internalIP;
        this.randomized = randomized;
    }

    public IPv4Address getInternalIP() {
        return internalIP;
    }
    
    public IPv4Address getExternalIP() { return internalIP; }
    
    public boolean isRandomized() {
        return randomized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        if (randomized != host.randomized) return false;
        return internalIP != null ? internalIP.equals(host.internalIP) : host.internalIP == null;
    }

    @Override
    public int hashCode() {
        int result = internalIP != null ? internalIP.hashCode() : 0;
        result = 31 * result + (randomized ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Host{" +
                "internalIP=" + internalIP +
                ", randomized=" + randomized +
                '}';
    }
}
