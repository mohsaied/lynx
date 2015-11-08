package lynx.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;

import lynx.analysis.Analysis;
import lynx.analysis.PerfAnalysis;
import lynx.clustering.NocClustering;
import lynx.data.Design;
import lynx.data.Noc;
import lynx.elaboration.ConnectionGroup;
import lynx.elaboration.Elaboration;
import lynx.interconnect.NocInterconnect;
import lynx.log.MyLogger;
import lynx.nocmapping.Mapping;
import lynx.nocmapping.NocMapping;
import lynx.graphics.Gui;
import lynx.hdlgen.Simulation;
import lynx.vcmapping.VcDesignation;
import lynx.vcmapping.VcMap;
import lynx.xml.XmlDesign;

public class Main {

    @SuppressWarnings("unused")
    public final static void main(String[] args) {

        // try {
        // MyLogger parentLog = new MyLogger(Level.ALL);
        // runFlow("D:\\Dropbox\\PhD\\Software\\noclynx\\designs\\simple\\simple.xml");
        // } catch (Exception e) {
        // e.printStackTrace();
        // ReportData.getInstance().writeToRpt("SCHMETTERLING");
        // ReportData.getInstance().writeToRpt(e.getMessage());
        // ReportData.getInstance().closeRpt();
        // }
        try {
            if (args.length == 0) { // bring up the GUI
                Gui gui = new Gui(null);
                MyLogger parentLog = new MyLogger(Level.ALL);
            } else if (args[0].equals("-c")) {
                // command line requested -- second argument is the designpath
                MyLogger parentLog = new MyLogger(Level.ALL);
                String filePath = args[1];
                runFlow(filePath);
            } else if (args[0].equals("--analysis")) {
                MyLogger parentLog = new MyLogger(Level.ALL);
                String inLynxTrace = args[1];
                String outReport = args[2];
                runAnalysis(inLynxTrace, outReport);
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

        DesignData.resetSingleton();
        ReportData.resetSingleton();
        ReportData.getInstance().setDesignFile(new File(filePath));

        // read XML design
        XmlDesign.readXMLDesign(filePath);

        // add NoC circuitry - NoC and translators
        NocInterconnect.addNoc(DesignData.getNocFilePath());

        // cluster design into SCCs
        NocClustering.clusterDesign();
        Design clusteredDesign = DesignData.getInstance().getClusteredDesign();

        List<ConnectionGroup> cgList = Elaboration.identifyConnectionGroups(clusteredDesign);
        DesignData.getInstance().setConnectionGroups(cgList);

        // find possible locations on the NoC
        Noc noc = DesignData.getInstance().getNoc();
        Mapping mapping = NocMapping.findMappings(clusteredDesign, noc);
        VcMap vcMap = VcDesignation.createVcMap(clusteredDesign, noc, mapping);
        DesignData.getInstance().setVcMap(vcMap);

        // write out XML design
        // XmlDesign.writeXMLDesign(design, filePath + ".out");

        // connect modules and insert translators
        NocInterconnect.connectDesignToNoc(clusteredDesign, noc, cgList, vcMap);

        // output simulation directory
        Design simulationDesign = DesignData.getInstance().getSimulationDesign();
        Simulation.generateSimDir(simulationDesign, noc);

        // parse lynx_trace and print some performance metrics
        File simRepFile = ReportData.getInstance().getSimRepFile();
        Analysis analysis = PerfAnalysis.parseSimFile(simRepFile);
        DesignData.getInstance().setAnalysis(analysis);

    }

    private static void runAnalysis(String inLynxTrace, String outReport) throws IOException {
        Analysis analysis = PerfAnalysis.parseSimFile(new File(inLynxTrace));
        PerfAnalysis.writeAnalysisReport(new PrintWriter(outReport), analysis);
    }

}
