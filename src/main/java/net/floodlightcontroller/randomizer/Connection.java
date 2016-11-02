package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 9/14/16.
 *
 * This is a connection object for the EAGER project.
 */
public class Connection {
    private static Logger log = LoggerFactory.getLogger(Connection.class);
    private Server server;
    private AbstractFlow encryptflow;
    private AbstractFlow decryptflow;
    private AbstractFlow arpflows = null;

    public Connection(Server server, DatapathId sw, OFPort wanport, OFPort localport, Boolean isRandomSide) {
        this.server = server;
        if (isRandomSide) {     //Todo install flows immediately
            arpflows = new ArpFlowsRandom(wanport, localport, sw);
            encryptflow = new EncryptSourceFlow(wanport, localport, sw);
            decryptflow = new DecryptDestinationFlow(wanport, localport, sw);
        } else {
            arpflows = new ArpFlowsNonRandom(wanport, localport, sw);
            encryptflow = new EncryptDestinationFlow(wanport, localport, sw);
            decryptflow = new DecryptSourceFlow(wanport, localport, sw);
        }
        log.debug("Inserting encrypt and decrypt flows for a new connection!");
        encryptflow.insertFlow(server);
        decryptflow.insertFlow(server);
    }

    public void update() {
        log.debug("Removing encrypt and inserting encrypt and decrypt flows for an existing connection!");
        arpflows.insertFlow(server);
        //encryptflow.removeFlow(server);
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
