package lynx.hdlgen;

import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Noc;

public class Simulation {

    private static final Logger log = Logger.getLogger(Simulation.class.getName());
    public static final boolean USE_SW_FABRICPORT = true;

    public static void generateSimDir(Design design, Noc noc) throws Exception {

        log.info("Simulation directory creation started");

        VerilogOut.writeVerilogTestBench(design, noc);
        NocConfigAndWrapperOut.writeNocConfigAndWrapper(noc);
        QuickScriptOut.writeQuickScript(design);
    }
}
