package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by geddingsbarrineau on 9/14/16.
 */
public class ServerManager {

    private List<Server> serverList;

    public ServerManager() {
        serverList = new ArrayList<Server>();
    }

    public void updateServers() {

    }

    public void addServer(Server server) {
        serverList.add(server);
    }

    public Server getServer(IPv4Address ip) {
        for (Server s : serverList) {
            if (s.getiPv4AddressReal().equals(ip)) return s;
        }
        return null;
    }

    public Server getServer(IPv6Address ip) {
        for (Server s : serverList) {
            if (s.getiPv6AddressReal().equals(ip)) return s;
        }
        return null;
    }

    public Server getServerFake(IPv4Address ip) {
        for (Server s : serverList) {
            if (s.getiPv4AddressFake().equals(ip)) return s;
        }
        return null;
    }

    public Server getServerFake(IPv6Address ip) {
        for (Server s : serverList) {
            if (s.getiPv6AddressFake().equals(ip)) return s;
        }
        return null;
    }
}
