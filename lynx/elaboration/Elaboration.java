package lynx.elaboration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.MyEnums.ConnectionType;

public class Elaboration {

    private static final Logger log = Logger.getLogger(Elaboration.class.getName());

    public static List<ConnectionGroup> identifyConnectionGroups(Design design) {
        log.info("Starting elaboration");

        List<ConnectionGroup> cgList = findConnectionGroups(design);

        log.info("Finished elaboration");

        return cgList;
    }

    private static List<ConnectionGroup> findConnectionGroups(Design design) {

        List<ConnectionGroup> cgList = new ArrayList<ConnectionGroup>();

        // get the list of connectons in this design
        List<Connection> conList = design.getConnections();

        // set with done connections
        Set<Connection> doneSet = new HashSet<Connection>();

        // there is a large connectiongroup for p2p connections because we don't
        // need to do much about them for now
        ConnectionGroup p2pGroup = new ConnectionGroup(ConnectionType.P2P);

        // loop over each connection and figure out its type
        // whenever we process a connection, mark it as done
        for (Connection con : conList) {
            // if we already processed this connection, move on
            if (doneSet.contains(con))
                continue;

            // for this connection, check its dst bundle
            // does the dstbundle have other incoming connections to it?
            Bundle dstBundle = con.getToBundle();
            // we're part of a multimaster connectiongroup if the dst bundle
            // that this connection feeds has multiple connections
            boolean multimaster = dstBundle.getConnections().size() > 1;
            if (multimaster) {
                log.info("Found arbitration connectiongroup");
                ConnectionGroup mmGroup = new ConnectionGroup(ConnectionType.ARBITRATION);
                for (Connection con1 : conList) {
                    if (con1.getToBundle() == dstBundle) {
                        log.info("\tAdding connection: " + con1);
                        mmGroup.addConnection(con1);
                        // mark connection as processed
                        doneSet.add(con1);
                    }
                }
                cgList.add(mmGroup);
            } else {
                log.info("Found p2p connection: " + con);
                p2pGroup.addConnection(con);
                doneSet.add(con);
            }
        }

        cgList.add(p2pGroup);

        return cgList;
    }
}
