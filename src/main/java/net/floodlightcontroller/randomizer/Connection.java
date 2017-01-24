package net.floodlightcontroller.randomizer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.randomizer.web.ConnectionSerializer;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 9/14/16.
 *
 * This is a connection object for the EAGER project.
 */
@JsonSerialize(using = ConnectionSerializer.class)
public class Connection {
    private static Logger log = LoggerFactory.getLogger(Connection.class);

    private Server server;
    private DatapathId sw;
    private FlowFactory flowFactory;

    public Connection(Server server, DatapathId sw) {
        this.server = server;
        this.sw = sw;
        flowFactory = new FlowFactory(server);

        IOFSwitch ofSwitch = Randomizer.switchService.getActiveSwitch(sw);
        for (OFFlowMod flow : flowFactory.getFlowAdds()) {
            ofSwitch.write(flow);
        }
    }

    public void update() {
        IOFSwitch ofSwitch = Randomizer.switchService.getActiveSwitch(sw);
        for (OFFlowMod flow : flowFactory.getFlowAdds()) {
            ofSwitch.write(flow);
        }
    }

    public Server getServer() {
        return server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connection that = (Connection) o;

        if (server != null ? !server.equals(that.server) : that.server != null) return false;
        return sw != null ? sw.equals(that.sw) : that.sw == null;
    }

    @Override
    public int hashCode() {
        int result = server != null ? server.hashCode() : 0;
        result = 31 * result + (sw != null ? sw.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "server=" + server +
                ", sw=" + sw +
                '}';
    }
}
