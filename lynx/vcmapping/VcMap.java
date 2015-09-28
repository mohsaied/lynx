package lynx.vcmapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.data.Bundle;

/**
 * Data structure to hold all information about the use of VCs. This includes:
 * which VC does each bundle send on? Which bundles are part of combine-data
 * mode?
 * 
 * @author Mohamed
 *
 */
public class VcMap {

    private Map<Bundle, Integer> bundleToVcs;
    private Map<Bundle, Integer> dstBundleToCombineData;
    private Map<Integer, Integer> routerToCombineData;

    public VcMap() {
        // init data structures
        bundleToVcs = new HashMap<Bundle, Integer>();
        dstBundleToCombineData = new HashMap<Bundle, Integer>();
        routerToCombineData = new HashMap<Integer, Integer>();
    }

    /**
     * 
     * @param srcBundle
     *            source of connection
     * @param dstBundle
     *            sink of connection
     * @param router
     *            router at which the sink is connected
     * @param vc
     *            the VC assigned to this connection
     * @param combineData
     *            the combine data value associated with the sink
     */
    public void addVcDesignation(Bundle dstBundle, List<Bundle> srcBundles, int router, int vc, int combineData) {

        bundleToVcs.put(dstBundle, vc);
        for (Bundle srcBundle : srcBundles)
            bundleToVcs.put(srcBundle, vc);

        dstBundleToCombineData.put(dstBundle, combineData);

        routerToCombineData.put(router, combineData);
    }

    public Map<Bundle, Integer> getBundleToVcs() {
        return bundleToVcs;
    }

    public Map<Bundle, Integer> getBundleToCombineData() {
        return dstBundleToCombineData;
    }

    public Map<Integer, Integer> getRouterToCombineData() {
        return routerToCombineData;
    }

    @Override
    public String toString() {

        String s = "";

        for (Bundle dstBun : dstBundleToCombineData.keySet()) {
            s += "Dst. Bundle " + dstBun.getFullName() + ", c_d = " + dstBundleToCombineData.get(dstBun) + ", vc = "
                    + bundleToVcs.get(dstBun) + "\n";
            s += "Src Bundles: ";
            List<Bundle> srcBuns = dstBun.getConnections();
            for (Bundle srcBun : srcBuns) {
                if (bundleToVcs.get(srcBun) == bundleToVcs.get(dstBun))
                    s += srcBun.getFullName() + " ,";
                else
                    assert false : "Src bundles have different VCs than dst bundles";
            }
            s += "\n";
        }

        return s;

    }
}
