package lynx.vcmapping;

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

    Map<Bundle, Integer> bundleToVcs;
    Map<Bundle, Integer> bundleToCombineData;

    public VcMap() {

    }

}
