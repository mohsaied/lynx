package lynx.elaboration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Connection;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.ConnectionType;
import lynx.data.MyEnums.Direction;

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
            Bundle slaveDstBun = con.getToBundle();
            // we're part of a multimaster connectiongroup if the dst bundle
            // that this connection feeds has multiple connections
            // and these connections also have responses
            int twoWayConnections = 0;
            DesignModule mod = slaveDstBun.getParentModule();
            // loop over module's bundles
            for (Bundle slaveSrcBun : mod.getBundles().values()) {
                if (slaveSrcBun.getDirection() == Direction.OUTPUT) {
                    // we found a bundle that outputs data
                    // now we need to make sure that there are multiple
                    // connections from other modules to this dstBundle, and
                    // return connections from this dstbundle back to the same
                    // module
                    twoWayConnections = 0;
                    for (Bundle masterDstBun : slaveSrcBun.getConnections()) {

                        // look for a way back to our slave bun
                        boolean foundWayBack = false;
                        for (Bundle masterSrcBun : masterDstBun.getParentModule().getBundles().values()) {
                            // if this module contains an outgoing bundle to our
                            // mod, then we're good
                            for (Bundle masterSrcBunDst : masterSrcBun.getConnections()) {
                                if (masterSrcBunDst == slaveDstBun) {
                                    foundWayBack = true;
                                    twoWayConnections++;
                                    break;
                                }
                            }
                            if (foundWayBack)
                                break;
                        }

                    }
                    if (twoWayConnections > 1)
                        break;
                }
            }
            boolean multimaster = twoWayConnections > 1;
            if (multimaster) {
                log.info("Found arbitration connectiongroup");
                ConnectionGroup mmGroup = new ConnectionGroup(ConnectionType.ARBITRATION);
                for (Connection con1 : conList) {
                    if (con1.getToBundle() == slaveDstBun) {
                        log.info("\tAdding connection: " + con1);
                        mmGroup.addMasterConnection(con1);
                        // mark connection as processed
                        doneSet.add(con1);
                        // need to find the connections coming back, if they are
                        // present
                        for (Connection con2 : conList) {
                            if ((con2.getFromBundle().getParentModule() == con1.getToBundle().getParentModule())
                                    && (con2.getToBundle().getParentModule() == con1.getFromBundle().getParentModule())) {
                                log.info("\tAdding connection: " + con2);
                                mmGroup.addSlaveConnection(con2);
                                doneSet.add(con2);
                            }
                        }
                    }
                }
                cgList.add(mmGroup);
            }
        }

        for (Connection con : conList) {
            if (!doneSet.contains(con)) {
                log.info("Found p2p connection: " + con);
                p2pGroup.addConnection(con);
                doneSet.add(con);
            }
        }

        cgList.add(p2pGroup);

        return cgList;
    }
}
