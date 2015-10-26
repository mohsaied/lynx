package lynx.elaboration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.MyEnums.BundleType;
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

            boolean multimaster = false;
            if ((con.getFromBundle().getBundleType() == BundleType.MASTER && con.getToBundle().getBundleType() == BundleType.SLAVE)
                    || (con.getFromBundle().getBundleType() == BundleType.SLAVE && con.getToBundle().getBundleType() == BundleType.MASTER))
                multimaster = true;

            if (multimaster) {
                log.info("Found arbitration connectiongroup");
                ConnectionGroup mmGroup = new ConnectionGroup(ConnectionType.ARBITRATION);

                // need to expand app graph from here (this connection's
                // bundles) and add anything connected to the same MM network

                // create a processing queue
                Queue<Connection> processingQueue = new LinkedList<Connection>();

                // add this connection's bundles to it
                processingQueue.add(con);

                while (!processingQueue.isEmpty()) {
                    log.fine("pqsize = " + processingQueue.size());
                    Connection currCon = processingQueue.remove();
                    log.fine("pqsize (ar) = " + processingQueue.size());

                    log.fine("=================");
                    log.fine("\t" + con);
                    log.fine("\t\t" + con.getFromBundle());
                    log.fine("\t\t" + con.getToBundle());
                    log.fine("------------------");

                    if (doneSet.contains(currCon)) {
                        log.fine("Doneset contains it!");
                        continue;
                    }

                    Bundle fromBun = currCon.getFromBundle();
                    Bundle toBun = currCon.getToBundle();

                    log.fine("fromtype = " + fromBun.getBundleType());
                    log.fine("totype = " + toBun.getBundleType());

                    // add the current connection to the mmGroup
                    if (fromBun.getBundleType() == BundleType.MASTER && toBun.getBundleType() == BundleType.SLAVE) {
                        log.info("\tAdding master " + currCon);
                        mmGroup.addMasterConnection(currCon);
                    } else if (fromBun.getBundleType() == BundleType.SLAVE && toBun.getBundleType() == BundleType.MASTER) {
                        log.info("\tAdding slave " + currCon);
                        mmGroup.addSlaveConnection(currCon);
                    }

                    // mark as processed
                    doneSet.add(currCon);

                    // loop over all connections coming out of the same bundles,
                    // also loop over all connections from the sisterbundles and
                    // and add them
                    for (Connection con1 : conList) {
                        if (!doneSet.contains(con1))
                            if ((con1.getFromBundle() == fromBun) || (con1.getToBundle() == toBun)
                                    || (con1.getFromBundle() == toBun.getSisterBundle())
                                    || (con1.getToBundle() == fromBun.getSisterBundle())) {
                                processingQueue.add(con1);
                            }
                    }
                }

                cgList.add(mmGroup);
            }
        }

        for (Connection con : conList) {
            if (!doneSet.contains(con)) {
                log.fine("Found p2p connection: " + con);
                p2pGroup.addConnection(con);
                doneSet.add(con);
            }
        }

        log.info("Found " + p2pGroup.getConnections().size() + " P2P connections, and " + (cgList.size())
                + " arbitration connection groups.");

        cgList.add(p2pGroup);

        return cgList;
    }
}
