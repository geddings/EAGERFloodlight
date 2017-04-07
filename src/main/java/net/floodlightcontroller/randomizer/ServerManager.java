package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.IPv4Address;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by geddingsbarrineau on 9/14/16.
 * <p>
 * This is the server manager for the EAGER project.
 */
public class ServerManager {

    private List<Server> serverList;

    public ServerManager() {
        serverList = new ArrayList<Server>();
    }

    public List<Server> getServers() {
        return serverList;
    }

    public void updateServers() {
        serverList.forEach(Server::update);
    }

    public void addServer(Server server) {
        serverList.add(server);
    }

    public void removeServer(Server server) {
        serverList.remove(server);
    }

    public Server getServerFromRealIP(IPv4Address ip) {
        for (Server s : serverList) {
            if (s.getiPv4AddressReal().equals(ip)) return s;
        }
        return null;
    }

    public Server getServerFromFakeIP(IPv4Address ip) {
        for (Server s : serverList) {
            if (s.getiPv4AddressFake().equals(ip)) return s;
        }
        return null;
    }

    public Server getServerThatContainsIP(IPv4Address ip) {
        for (Server s : serverList) {
            if (s.getPrefix().contains(ip)) return s;
        }
        return null;
    }
    
    public Server getServer(IPv4Address ip) {
        for (Server s : serverList) {
            if (s.getiPv4AddressReal().equals(ip)) return s;
            else if (s.getiPv4AddressFake().equals(ip)) return s;
        }
        return null;
    }

}
