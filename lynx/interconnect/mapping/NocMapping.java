package lynx.interconnect.mapping;

import lynx.data.Design;
import lynx.interconnect.mapping.Ullman;

/**
 * Algorithms to map a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class NocMapping {

    public static void findMappings(Design design) {
        Ullman.ullmanFindMappings(design);
    }

}
