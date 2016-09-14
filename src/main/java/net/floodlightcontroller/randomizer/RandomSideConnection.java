package net.floodlightcontroller.randomizer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Created by geddingsbarrineau on 9/14/16.
 */
public class RandomSideConnection {

    Server server;
    EncryptSourceFlow encryptflow;
    DecryptDestinationFlow decryptflow;
    DecryptDestinationFlow decryptflowprev;
    DecryptDestinationFlow decryptflownext;

    public RandomSideConnection(Server server, DatapathId sw, OFPort wanport, OFPort hostport) {
        this.server = server;
        encryptflow = new EncryptSourceFlow(wanport, hostport, sw);
        decryptflow = new DecryptDestinationFlow(wanport, hostport, sw);
        encryptflow.insertFlow(server);
        decryptflow.insertFlow(server);
    }

    public void update() {
        encryptflow.removeFlow(server);
        encryptflow.insertFlow(server);
        decryptflow.insertFlow(server);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RandomSideConnection that = (RandomSideConnection) o;

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
