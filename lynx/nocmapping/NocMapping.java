package lynx.nocmapping;

import lynx.data.Design;

/**
 * Algorithms to map a design onto an NoC
 *
 * @author Mohamed
 * 
 */
public class NocMapping {

    public static void findMappings(Design design) {

        //Ullman.findMappings(design);

        SimulatedAnnealing.findMappings(design);
    }

}
