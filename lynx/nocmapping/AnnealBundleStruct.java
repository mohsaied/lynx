package lynx.nocmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lynx.data.Bundle;
import lynx.data.Design;
import lynx.data.DesignModule;
import lynx.data.Noc;
import lynx.data.NocBundle;
import lynx.data.MyEnums.Direction;

/**
 * Struct to hold the data structures required for SimulatedAnnealingBundle
 * 
 * @param bundleMap
 *            : a mapping between bundle --> nocbundle
 * @param usedNocBundle
 *            : boolean per nocbundle indicating whether it is used or not
 * @param bundlesAtRouter
 *            : set of bundles at each router index
 * 
 * @author Mohamed
 *
 */
public class AnnealBundleStruct {

    public Map<Bundle, List<NocBundle>> bundleMap;
    public Map<NocBundle, Boolean> usedNocBundle;
    public ArrayList<HashSet<Bundle>> bundlesAtRouter;

    // copy constructor initializes the struct with copies of maps contents
    public AnnealBundleStruct(AnnealBundleStruct original) {
        // make a copy of the bundleMap
        bundleMap = new HashMap<Bundle, List<NocBundle>>();
        for (Bundle bun : original.bundleMap.keySet()) {
            bundleMap.put(bun, original.bundleMap.get(bun));
        }

        // make a copy of the used nocBundle map
        usedNocBundle = new HashMap<NocBundle, Boolean>();
        for (NocBundle nocbun : original.usedNocBundle.keySet()) {
            usedNocBundle.put(nocbun, original.usedNocBundle.get(nocbun));
        }

        // make a copy of the bundles at each router
        bundlesAtRouter = new ArrayList<HashSet<Bundle>>();
        for (HashSet<Bundle> bunList : original.bundlesAtRouter) {
            HashSet<Bundle> newBunSet = new HashSet<Bundle>();
            for (Bundle bun : bunList) {
                newBunSet.add(bun);
            }
            bundlesAtRouter.add(newBunSet);
        }
    }

    public AnnealBundleStruct(Design design, Noc noc) {
        // mapping from bundle -> nocbundles
        bundleMap = new HashMap<Bundle, List<NocBundle>>();

        // map of used nocbundles
        usedNocBundle = new HashMap<NocBundle, Boolean>();

        List<Bundle> bundleList = design.getAllBundles();

        // initial solution: just put everything off-noc: empty mapping
        for (Bundle bun : bundleList) {
            bundleMap.put(bun, new ArrayList<NocBundle>());
        }

        // and all nocbundles are unused
        for (int i = 0; i < noc.getNumRouters(); i++) {
            for (NocBundle nocbun : noc.getNocInBundles(i)) {
                usedNocBundle.put(nocbun, false);
            }
            for (NocBundle nocbun : noc.getNocOutBundles(i)) {
                usedNocBundle.put(nocbun, false);
            }
        }

        // all routers have zero bundles on them except the last one
        bundlesAtRouter = new ArrayList<HashSet<Bundle>>();
        for (int i = 0; i < noc.getNumRouters(); i++) {
            HashSet<Bundle> bunSet = new HashSet<Bundle>();
            bundlesAtRouter.add(bunSet);
        }
        // put all bundles in the last router (off-noc)
        HashSet<Bundle> bunSet = new HashSet<Bundle>();
        for (Bundle bun : bundleList) {
            bunSet.add(bun);
        }
        bundlesAtRouter.add(bunSet);
    }

    // default constructor
    public AnnealBundleStruct() {
        bundleMap = new HashMap<Bundle, List<NocBundle>>();
        usedNocBundle = new HashMap<NocBundle, Boolean>();
        bundlesAtRouter = new ArrayList<HashSet<Bundle>>();
    }

    public void disconnectBundle(Bundle selectedBundle) {
        // first get the used nocbundles
        List<NocBundle> nocbunList = this.bundleMap.get(selectedBundle);
        // set these nocBundles as unused
        for (NocBundle nocbun : nocbunList) {
            this.usedNocBundle.put(nocbun, false);
        }
        // insert empty list to indicate unconnectedness
        List<NocBundle> emptyNocBundleList = new ArrayList<NocBundle>();
        this.bundleMap.put(selectedBundle, emptyNocBundleList);
        // remove bundle from the router or off-noc mapping
        if (nocbunList.size() > 0) {
            int oldRouterIndex = nocbunList.get(0).getRouter();
            this.bundlesAtRouter.get(oldRouterIndex).remove(selectedBundle);
        } else {
            this.bundlesAtRouter.get(bundlesAtRouter.size() - 1).remove(selectedBundle);
        }
    }

    public void connectBundle(Bundle selectedBundle, int selectedRouter, Noc noc) throws Exception {

        // that means we're mapping to a target off-noc
        if (selectedRouter == noc.getNumRouters()) {
            // add to off-noc list
            this.bundlesAtRouter.get(noc.getNumRouters()).add(selectedBundle);
            // that means we're mapping to a target on-noc
        } else {

            int requiredBundles = attemptMapping(selectedBundle, selectedRouter, noc);

            if (requiredBundles != 0) {

                // rip out some bundles to make room for this move
                ripOutSomeBundles(selectedBundle, selectedRouter, requiredBundles);

                // attempt to map again
                requiredBundles = attemptMapping(selectedBundle, selectedRouter, noc);

                assert requiredBundles == 0 : "Something's wrong! Could not assign " + requiredBundles
                        + " nocbundles, at router " + selectedRouter + " for bundle: " + selectedBundle.getFullName();
                if (requiredBundles != 0)
                    throw new Exception();
            }
        }
    }

    private void ripOutSomeBundles(Bundle selectedBundle, int selectedRouter, int requiredBundles) {
        List<Bundle> markedForRemoval = new ArrayList<Bundle>();
        // choose a bundle to rip out from this router
        for (Bundle bun : this.bundlesAtRouter.get(selectedRouter)) {

            // don't rip out a bundle of the opposite direction
            if (bun.getDirection() != selectedBundle.getDirection())
                continue;

            // mark this bundle for removal from bundlesAtRouter
            markedForRemoval.add(bun);

            // mark its nocbundles as unused
            for (NocBundle nocbun : this.bundleMap.get(bun)) {
                this.usedNocBundle.put(nocbun, false);
                requiredBundles--;
            }
            List<NocBundle> emptyNocBundleList1 = new ArrayList<NocBundle>();
            this.bundleMap.put(bun, emptyNocBundleList1);
            // if we have removed enough -> exit
            if (requiredBundles <= 0)
                break;
        }

        for (Bundle bun : markedForRemoval) {
            // remove this bundle from bundlesAtRouter
            this.bundlesAtRouter.get(selectedRouter).remove(bun);
        }
    }

    public int attemptMapping(Bundle selectedBundle, int selectedRouter, Noc noc) throws Exception {

        // map to selected router if possible without removing other bundles
        // get the nocbundles at the selected router
        // how many target nocbuns do we need?
        int bunWidth = selectedBundle.getWidth();
        assert bunWidth <= noc.getInterfaceWidth() : "Cannot (currently) handle bundles that are larger than NoC interface width of "
                + noc.getInterfaceWidth();
        if (bunWidth > noc.getInterfaceWidth())
            throw new Exception();

        // add selected Bundle to the current list of bundles at this router
        this.bundlesAtRouter.get(selectedRouter).add(selectedBundle);

        // how many noc bundles do I need?
        // input noc bundles are equal to flit(noc) width
        // output noc bundles are equal to fabric width/numVCs - more or less
        // TODO put that calculation in the Noc object because it doesn't belong
        // here
        // TODO also need a more general formula than that
        Direction selectedBundleDirection = selectedBundle.getDirection();
        int nocBundleWidth = 0;
        if (selectedBundleDirection == Direction.INPUT)
            nocBundleWidth = noc.getInterfaceWidth() / noc.getNumVcs();
        else
            nocBundleWidth = noc.getWidth();

        int numNocBundlesRequired = bunWidth / nocBundleWidth + (bunWidth % nocBundleWidth == 0 ? 0 : 1);

        // get the target nocbuns at the selected router
        List<NocBundle> targetNocbuns = selectedBundleDirection == Direction.INPUT ? noc.getNocOutBundles(selectedRouter) : noc
                .getNocInBundles(selectedRouter);

        // do we have enough nocbuns available in there?
        List<NocBundle> nocBundles = new ArrayList<NocBundle>();
        for (NocBundle nocbun : targetNocbuns) {
            if (!this.usedNocBundle.get(nocbun)) {
                nocBundles.add(nocbun);
                numNocBundlesRequired--;
                if (numNocBundlesRequired == 0)
                    break;
            }
        }

        // have we found a valid mapping?
        if (numNocBundlesRequired == 0) {
            this.bundleMap.put(selectedBundle, nocBundles);
            for (NocBundle nocbun : nocBundles) {
                this.usedNocBundle.put(nocbun, true);
            }
            this.bundlesAtRouter.get(selectedRouter).add(selectedBundle);
        }

        return numNocBundlesRequired;
    }

    public Set<Integer> getRoutersForBundle(Bundle bun) {
        Set<Integer> routers = new HashSet<Integer>();
        for (NocBundle nocbun : bundleMap.get(bun)) {
            routers.add(nocbun.getRouter());
        }
        return routers;
    }

    public int getRouterForGroupedModule(DesignModule mod, Noc noc) {
        for (Bundle bun : mod.getBundles().values()) {
            Set<Integer> routersForBundle = getRoutersForBundle(bun);
            if (routersForBundle.size() == 0)
                return noc.getNumRouters();
            else
                return (int) getRoutersForBundle(bun).toArray()[0];
        }
        assert false : "wth?";
        return -1;
    }
}
