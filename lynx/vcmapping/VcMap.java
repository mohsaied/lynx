package lynx.vcmapping;

import java.util.HashMap;
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
    public void addVcDesignation(Bundle srcBundle, Bundle dstBundle, int router, int vc, int combineData) {

        bundleToVcs.put(srcBundle, vc);
        bundleToVcs.put(dstBundle, vc);

        dstBundleToCombineData.put(dstBundle, combineData);

        routerToCombineData.put(router, vc);
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

}
