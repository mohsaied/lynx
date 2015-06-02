package lynx.main;

import java.util.List;

import lynx.analysis.Analysis;
import lynx.clustering.Clustering;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.elaboration.ConnectionGroup;
import lynx.nocmapping.Mapping;

/**
 * This singleton class holds all design info for the flow when using an NoC
 * 
 * @author Mohamed
 *
 */
public class DesignData {

    /**
     * The user-entered design
     */
    Design design;

    /**
     * The specified NoC
     */
    Noc noc;

    /**
     * clustering information
     */
    Clustering clustering;

    /**
     * Design post-clustering
     */
    Design clusteredDesign;

    /**
     * the design mapping from clustered Module to router
     */
    Mapping nocMapping;

    /**
     * A design instance with src/sink/via instead of actual modules
     */
    Design simulationDesign;

    /**
     * Analysis of the opened design
     */
    Analysis analysis;

    /**
     * List of ConnectionGroups in this design
     */
    private List<ConnectionGroup> connectionGroups;

    /**
     * Singleton of design data
     */
    private static DesignData instance = null;

    private DesignData() {
        this.design = null;
        this.clusteredDesign = null;
        this.noc = null;
        this.nocMapping = null;
    }

    public static DesignData getInstance() {
        if (instance == null)
            instance = new DesignData();
        return instance;
    }

    public static void resetSingleton() {
        instance = null;
    }

    public final Design getDesign() {
        return design;
    }

    public final void setDesign(Design design) {
        this.design = design;
        design.update();
    }

    public final Noc getNoc() {
        return noc;
    }

    public final void setNoc(Noc noc) {
        this.noc = noc;
    }

    public final Design getClusteredDesign() {
        return clusteredDesign;
    }

    public final void setClusteredDesign(Design clusteredDesign) {
        this.clusteredDesign = clusteredDesign;
    }

    public final Design getSimulationDesign() {
        return simulationDesign;
    }

    public final void setSimulationDesign(Design simulationDesign) {
        this.simulationDesign = simulationDesign;
    }

    public final Mapping getNocMapping() {
        return nocMapping;
    }

    public final void setNocMapping(Mapping nocMapping) {
        this.nocMapping = nocMapping;
    }

    public final Analysis getAnalysis() {
        return analysis;
    }

    public final void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public void setConnectionGroups(List<ConnectionGroup> cgList) {
        this.connectionGroups = cgList;
    }

    public List<ConnectionGroup> getConnectionGroups() {
        return connectionGroups;
    }

}
