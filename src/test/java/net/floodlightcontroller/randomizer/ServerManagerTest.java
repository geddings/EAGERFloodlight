package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.test.FloodlightTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;

/**
 * Created by geddingsbarrineau on 2/2/17.
 */
public class ServerManagerTest extends FloodlightTestCase{

    Server server;
    ServerManager sm;
    OFFactory factory;
    
    @Before
    public void SetUp() throws Exception {
        super.setUp();
        sm = new ServerManager();
        server = new Server(IPv4Address.of(10, 0, 0, 4), IPv4AddressWithMask.of("184.164.243.0/24"));
        sm.addServer(server);
        factory = OFFactories.getFactory(OFVersion.OF_13);
    }
    
    @Test
    public void testGetServerThatContainsIP() {
        Server actual = sm.getServerThatContainsIP(IPv4Address.of("184.164.243.69"));
        Assert.assertEquals(server, actual);
    }
    
}
