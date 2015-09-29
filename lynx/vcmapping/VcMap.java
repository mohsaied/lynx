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

    /**
     * a map that tells which VC each bundle is using. If the value of the VC is
     * -1, this indicates that the SRC bundle can send on multiple VCs to
     * different destinations. To find out the dst-vc table, look at the
     * dstbundle VCs
     */
    private Map<Bundle, Integer> bundleToVcs;
    /**
     * the combine data status of each dstbundle. The integer tells how many
     * bundles are being combined here
     */
    private Map<Bundle, Integer> dstBundleToCombineData;
    /**
     * combine_data status per router. The integer tells how many bundles are
     * being combined here
     */
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
        for (Bundle srcBundle : srcBundles) {
            if (!bundleToVcs.containsKey(srcBundle)) {
                bundleToVcs.put(srcBundle, vc);
            } else if (bundleToVcs.get(srcBundle) != vc) {
                // this src bundle sends on multiple VCs
                bundleToVcs.put(srcBundle, -1);
            }
        }

        dstBundleToCombineData.put(dstBundle, combineData);

        routerToCombineData.put(router, combineData);
    }

    public int getVcForBundle(Bundle bundle) {
        if (bundleToVcs.containsKey(bundle))
            return bundleToVcs.get(bundle);
        else
            // default VC is 0 if it was never added
            return 0;
    }

    @Override
    public String toString() {

        String s = "VC MAP:\n";

        for (Bundle dstBun : dstBundleToCombineData.keySet()) {
            s += "Dst. Bundle " + dstBun.getFullName() + ", c_d = " + dstBundleToCombineData.get(dstBun) + ", vc = "
                    + bundleToVcs.get(dstBun) + "\n";
            s += "  Src Bundles: ";
            List<Bundle> srcBuns = dstBun.getConnections();
            for (Bundle srcBun : srcBuns) {
                if (bundleToVcs.get(srcBun) == bundleToVcs.get(dstBun)) {
                    s += srcBun.getFullName() + " ,";
                } else {
                    s += srcBun.getFullName() + "(" + bundleToVcs.get(srcBun) + "),";
                }
            }
            s += "\n";
        }

        return s.substring(0, s.length() - 1);

    }

    public final Map<Bundle, Integer> getBundleToVcs() {
        return bundleToVcs;
    }

    public final void setBundleToVcs(Map<Bundle, Integer> bundleToVcs) {
        this.bundleToVcs = bundleToVcs;
    }

    public final Map<Bundle, Integer> getDstBundleToCombineData() {
        return dstBundleToCombineData;
    }

    public final void setDstBundleToCombineData(Map<Bundle, Integer> dstBundleToCombineData) {
        this.dstBundleToCombineData = dstBundleToCombineData;
    }

    public final Map<Integer, Integer> getRouterToCombineData() {
        return routerToCombineData;
    }

    public final void setRouterToCombineData(Map<Integer, Integer> routerToCombineData) {
        this.routerToCombineData = routerToCombineData;
    }
}
