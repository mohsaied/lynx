package lynx.main;

import java.io.File;
import java.util.logging.Level;

import lynx.analysis.Analysis;
import lynx.analysis.PerfAnalysis;
import lynx.clustering.NocClustering;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.interconnect.NocInterconnect;
import lynx.log.MyLogger;
import lynx.nocmapping.NocMapping;
import lynx.graphics.Gui;
import lynx.hdlgen.Simulation;
import lynx.xml.XmlDesign;

public class Main {

    @SuppressWarnings("unused")
    public final static void main(String[] args) {

//        try {
//            MyLogger parentLog = new MyLogger(Level.ALL);
//            runFlow("D:\\Dropbox\\PhD\\Software\\noclynx\\designs\\simple\\simple.xml");
//        } catch (Exception e) {
//            e.printStackTrace();
//            ReportData.getInstance().writeToRpt("SCHMETTERLING");
//            ReportData.getInstance().writeToRpt(e.getMessage());
//            ReportData.getInstance().closeRpt();
//        }

        try {
            if (args.length == 0) { // bring up the GUI
                Gui gui = new Gui(null);
                MyLogger parentLog = new MyLogger(Level.ALL);
            } else if (args[0].equals("-c")) {
                // command line requested -- second argumentis the designpath
                MyLogger parentLog = new MyLogger(Level.ALL);
                String filePath = args[1];
                runFlow(filePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ReportData.getInstance().writeToRpt("SCHMETTERLING");
            ReportData.getInstance().writeToRpt(e.getMessage());
            ReportData.getInstance().closeRpt();
        }
    }

    /**
     * The command line "automatic" flow
     * 
     * @param filePath
     * 
     */
    private static void runFlow(String filePath) throws Exception {

        ReportData.getInstance().setDesignFile(new File(filePath));

        // read XML design
        XmlDesign.readXMLDesign(filePath);

        // add NoC circuitry - NoC and translators
        NocInterconnect.addNoc("nocs/w150_n16_v2_d16.xml");

        // cluster design into SCCs
        NocClustering.clusterDesign();

        // find possible locations on the NoC
        Design clusteredDesign = DesignData.getInstance().getClusteredDesign();
        Noc noc = DesignData.getInstance().getNoc();
        NocMapping.findMappings(clusteredDesign, noc);

        // write out XML design
        // XmlDesign.writeXMLDesign(design, filePath + ".out");

        // connect modules and insert translators
        NocInterconnect.connectDesignToNoc(clusteredDesign, noc);

        // output simulation directory
        Design simulationDesign = DesignData.getInstance().getSimulationDesign();
        Simulation.generateSimDir(simulationDesign, noc);

        // parse lynx_trace and print some performance metrics
        File simRepFile = ReportData.getInstance().getSimRepFile();
        Analysis analysis = PerfAnalysis.parseSimFile(simRepFile);
        DesignData.getInstance().setAnalysis(analysis);
        
    }

}
