package lynx.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.MyEnums.Direction;

/**
 * 
 * Implementation of tarjan's clustering algorithm for strongly-connected graph
 * components.
 * 
 * @author Mohamed
 *
 */
public class Tarjan {

    private static final Logger log = Logger.getLogger(Tarjan.class.getName());

    // hashmap of module indicies and lowlink
    private static Map<String, Integer> moduleIndices;
    private static Map<String, Integer> moduleLowlink;

    // current lowest index
    private static int currIndex;

    // traversal stack - do everything by name (our problems aren't large)
    private static Stack<DesignModule> traversalStack;
    private static Map<String, Boolean> onStack;

    // output set of strongly connected components
    private static List<Set<String>> stronglyConnectedComponents;

    public static Design clusterDesign(Design design) {

        log.info("Running Tarjan's algorithm to cluster strongly-connected components...");

        // create a new design and give it the same NoC that we have
        Design clusteredDesign = new Design(design.getName() + "_clustered");
        clusteredDesign.setNoc(design.getNoc());

        // init data structures
        moduleIndices = new HashMap<String, Integer>();
        moduleLowlink = new HashMap<String, Integer>();
        onStack = new HashMap<String, Boolean>();
        traversalStack = new Stack<DesignModule>();
        stronglyConnectedComponents = new ArrayList<Set<String>>();

        // start at index 0
        currIndex = 0;

        // initialize maps to -1s (not explored)
        for (String modName : design.getDesignModules().keySet()) {
            moduleIndices.put(modName, -1);
            moduleLowlink.put(modName, -1);
            onStack.put(modName, false);
        }

        // go over the original design and look for strongly-connected parts
        for (String modName : design.getDesignModules().keySet()) {
            // call strongconnect on this node if we haven't already
            if (moduleIndices.get(modName) == -1) {
                strongConnect((DesignModule) design.getModuleByName(modName));
            }
        }

        // debug print the SCCs
        // debugPrint(stronglyConnectedComponents);

        return clusteredDesign;

    }

    private static void strongConnect(DesignModule module) {

        String modName = module.getName();

        // set the depth index for current module to be the smallest unused one
        moduleIndices.put(modName, currIndex);
        moduleLowlink.put(modName, currIndex);
        currIndex++;

        // push onto stack to explore
        traversalStack.push(module);
        onStack.put(modName, true);

        // loop over successors of current module, to do this we need to go over
        // all outbuns and get the modules at the other ends
        for (Bundle srcBun : module.getBundles().values())
            if (srcBun.getDirection() == Direction.OUTPUT)
                for (Bundle dstBun : srcBun.getConnections()) {

                    // this is a child module connected to our current module
                    DesignModule childMod = dstBun.getParentModule();
                    String childModName = childMod.getName();

                    // if childmod has no index --> recurse
                    if (moduleIndices.get(childModName) == -1) {
                        strongConnect(childMod);

                        // update low link if one of the children is connected
                        // to a lower link
                        int childLowlink = moduleLowlink.get(childModName);
                        if (childLowlink < moduleLowlink.get(modName)) {
                            moduleLowlink.put(modName, childLowlink);
                        }
                    } else if (onStack.get(childModName)) {
                        // already on stack, therefore in the current strongly
                        // connected component (SCC)
                        // update lowlink
                        int childLowlink = moduleLowlink.get(childModName);
                        if (childLowlink < moduleLowlink.get(modName)) {
                            moduleLowlink.put(modName, childLowlink);
                        }
                    }

                }

        // after looking at successors
        // check if the current module was a root
        // (that means its index is equal to it's lowlink)
        if (moduleIndices.get(modName) == moduleLowlink.get(modName)) {
            // create new scc
            Set<String> currScc = new HashSet<String>();

            // pop modules off the stack and insert into SCC
            while (!traversalStack.isEmpty()) {

                // pop vertex off stack
                DesignModule currMod = traversalStack.pop();
                String currModName = currMod.getName();
                onStack.put(currModName, false);

                // add to current SCC
                currScc.add(currModName);

                // stop after adding root node
                if (currModName.equals(modName))
                    break;
            }

            // output current SCC
            stronglyConnectedComponents.add(currScc);

        }
    }

    @SuppressWarnings("unused")
    private static void debugPrint(List<Set<String>> stronglyConnectedComponents2) {
        for (int i = 0; i < stronglyConnectedComponents.size(); i++) {
            Set<String> currScc = stronglyConnectedComponents.get(i);
            System.out.print("SCC " + i + ": ");
            for (String currModName : currScc) {
                System.out.print(currModName + " ");
            }
            System.out.println();
        }
    }
}
