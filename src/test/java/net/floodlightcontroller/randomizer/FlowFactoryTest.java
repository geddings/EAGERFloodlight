package net.floodlightcontroller.randomizer;

import net.floodlightcontroller.test.FloodlightTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;

/**
 * Created by geddingsbarrineau on 1/27/17.
 */
public class FlowFactoryTest extends FloodlightTestCase {
    
    FlowFactory ff;
    Server server;
    OFFactory factory;
    
    @Before 
    public void SetUp() throws Exception {
        super.setUp();
        
        server = new Server(IPv4Address.of(20, 0, 0, 4));
        ff = new FlowFactory(server);
        factory = OFFactories.getFactory(OFVersion.OF_13);
    }
    
    @Test
    public void testGetMatchNonRandomEncryptIPv4() {
        FlowFactory.setRandomize(false);
        FlowFactory.RewriteFlow rewriteflow = new FlowFactory.RewriteFlow(FlowFactory.FlowType.ENCRYPT, EthType.IPv4);
        Match expected = factory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, server.getiPv4AddressReal())
                .build();
        Match actual = ff.getMatch(rewriteflow);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetMatchRandomEncryptIPv4() {
        FlowFactory.setRandomize(true);
        FlowFactory.RewriteFlow rewriteflow = new FlowFactory.RewriteFlow(FlowFactory.FlowType.ENCRYPT, EthType.IPv4);
        Match expected = factory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, server.getiPv4AddressReal())
                .build();
        Match actual = ff.getMatch(rewriteflow);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetMatchNonRandomDecryptIPv4() {
        FlowFactory.setRandomize(false);
        FlowFactory.RewriteFlow rewriteflow = new FlowFactory.RewriteFlow(FlowFactory.FlowType.DECRYPT, EthType.IPv4);
        Match expected = factory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_SRC, server.getiPv4AddressFake())
                .build();
        Match actual = ff.getMatch(rewriteflow);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetMatchRandomDecryptIPv4() {
        FlowFactory.setRandomize(true);
        FlowFactory.RewriteFlow rewriteflow = new FlowFactory.RewriteFlow(FlowFactory.FlowType.DECRYPT, EthType.IPv4);
        Match expected = factory.buildMatch().setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.IPV4_DST, server.getiPv4AddressFake())
                .build();
        Match actual = ff.getMatch(rewriteflow);
        Assert.assertEquals(expected, actual);
    }
}
