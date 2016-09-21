package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Created by geddingsbarrineau on 9/14/16.
 */
public class Connection {

    Server server;
    AbstractFlow encryptflow;
    AbstractFlow decryptflow;
    AbstractFlow decryptflowprev;
    AbstractFlow decryptflownext;
    ArpFlows arpflows = null;

    public Connection(Server server, DatapathId sw, OFPort wanport, OFPort localport, Boolean isRandomSide) {
        this.server = server;
        if (isRandomSide) {     //Todo install flows immediately
            arpflows = new ArpFlows(wanport, localport, sw);
            encryptflow = new EncryptSourceFlow(wanport, localport, sw);
            decryptflow = new DecryptDestinationFlow(wanport, localport, sw);
        } else {
            encryptflow = new EncryptDestinationFlow(wanport, localport, sw);
            decryptflow = new DecryptSourceFlow(wanport, localport, sw);
        }
        encryptflow.insertFlow(server);
        decryptflow.insertFlow(server);
    }

    public void update() {
        if (arpflows != null) arpflows.insertFlow(server);
        encryptflow.removeFlow(server);
        encryptflow.insertFlow(server);
        decryptflow.insertFlow(server);
    }

    public Server getServer() {
        return server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connection that = (Connection) o;

        return server != null ? server.equals(that.server) : that.server == null;

    }

    @Override
    public int hashCode() {
        return server != null ? server.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RandomSideConnection{" +
                "server=" + server +
                ", encryptflow=" + encryptflow +
                ", decryptflow=" + decryptflow +
                '}';
    }
}
