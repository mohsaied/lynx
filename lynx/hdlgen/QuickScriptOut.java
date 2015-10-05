package lynx.hdlgen;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.main.ReportData;

/**
 * Output the noc_config file that goes into rtl2booksim
 * 
 * @author Mohamed
 *
 */
public class QuickScriptOut {

    private static final Logger log = Logger.getLogger(QuickScriptOut.class.getName());

    public static void writeQuickScript(Design design) throws FileNotFoundException, UnsupportedEncodingException {

        PrintWriter writer = ReportData.getInstance().getQuickScriptFile();
        String simDir = ReportData.getInstance().getSimDirString();
        String vlogDir = ReportData.getInstance().getVlogDirString();

        log.info("Generating simulation quick_script");

        writer.println("#!/bin/bash");
        writer.println();
        writer.println("export PATH=$PATH:/home/mohamed/altera/14.0/modelsim_ase/bin");
        writer.println();
        writer.println("HNOCSIM_DIR='/home/mohamed/Dropbox/PhD/Software/simulator/hnocsim'");
        writer.println("DESIGN_DIR='" + simDir + "'");
        writer.println("VERILOG_COMMON_DIR='" + vlogDir + "'");
        writer.println();
        writer.println("#remake booksim");
        writer.println("cd $HNOCSIM_DIR/booksim");
        writer.println("make");
        writer.println("cd $DESIGN_DIR");
        writer.println();
        writer.println("#create work library");
        writer.println("rm -rf work");
        writer.println("vlib work");
        writer.println();
        writer.println("#######################");
        writer.println("#compile verilog files");
        writer.println("#######################");
        writer.println();
        writer.println("#compile the rtl interface that talks to booksim's socket interface");
        writer.println("vlog -dpiheader $HNOCSIM_DIR/booksim/dpi.h $HNOCSIM_DIR/booksim/rtl_interface.sv");
        writer.println();
        writer.println("#compile the fabric port");
        writer.println("vlog  $HNOCSIM_DIR/fabric_port/fabric_port_in/*.sv");
        writer.println("vlog  $HNOCSIM_DIR/fabric_port/fabric_port_out/*.sv");
        writer.println();
        writer.println("#compile the translators and tpg/oras");
        writer.println("vlog  $VERILOG_COMMON_DIR/translators/common/*.sv");
        writer.println("vlog  $VERILOG_COMMON_DIR/translators/standard/*.sv");
        writer.println("vlog  $VERILOG_COMMON_DIR/translators/vc_table/*.sv");
        writer.println("vlog  $VERILOG_COMMON_DIR/translators/vc_direct/*.sv");
        writer.println("vlog  $VERILOG_COMMON_DIR/bfms/*.sv");
        writer.println("vlog  $VERILOG_COMMON_DIR/credit_shells/*.sv");
        writer.println();
        writer.println("#compile the fabric interface which instantiates fabric ports and an rtl interface");
        writer.println("vlog $HNOCSIM_DIR/booksim/fabric_interface.sv");
        writer.println("vlog -dpiheader $HNOCSIM_DIR/booksim/dpi_fabric.h $HNOCSIM_DIR/booksim/fabric_interface_sw.sv");
        writer.println();
        writer.println("#compile your design files (and testbenches) here");
        writer.println("vlog $DESIGN_DIR/*.sv");
        writer.println();
        writer.println("########################################");
        writer.println("#recompile the booksim socket interface");
        writer.println("########################################");
        writer.println();
        if (Simulation.USE_SW_FABRICPORT) {
            writer.println("g++ -c -fPIC -m32 -I/home/mohamed/altera/14.0/modelsim_ase/include $HNOCSIM_DIR/booksim/fabric_sw_interface.cpp");
            writer.println("g++ -shared -Bsymbolic -fPIC -m32 -o fabric_sw_interface.so fabric_sw_interface.o");
        } else {
            writer.println("g++ -c -fPIC -m32 -I/home/mohamed/altera/14.0/modelsim_ase/include $HNOCSIM_DIR/booksim/booksim_interface.cpp");
            writer.println("g++ -shared -Bsymbolic -fPIC -m32 -o booksim_interface.so booksim_interface.o");
        }
        writer.println();
        writer.println("###############################");
        writer.println("#run booksim in a new terminal");
        writer.println("###############################");
        writer.println();
        writer.println("if [ \"$1\" == \"keep_open\" ]; then");
        writer.println("    gnome-terminal --window-with-profile=keep_open -e $HNOCSIM_DIR/booksim/booksim\\ noc_config &");
        writer.println("else");
        writer.println("    gnome-terminal -e $HNOCSIM_DIR/booksim/booksim\\ noc_config &");
        writer.println("fi");
        writer.println();
        writer.println("################");
        writer.println("#run simulation");
        writer.println("################");
        writer.println();
        writer.println("if [ \"$1\" == \"vsim\" ]; then");
        if (Simulation.USE_SW_FABRICPORT)
            writer.println("    vsim -sv_lib fabric_sw_interface testbench -do wave.do ");
        else
            writer.println("    vsim -sv_lib booksim_interface testbench -do wave.do ");
        writer.println("else");
        if (Simulation.USE_SW_FABRICPORT)
            writer.println("    vsim -c -sv_lib fabric_sw_interface testbench -do \"run -all\"");
        else
            writer.println("    vsim -c -sv_lib booksim_interface testbench -do \"run -all\"");
        writer.println("fi");
        writer.println();
        writer.println("#########");
        writer.println("#cleanup");
        writer.println("#########");
        writer.println();
        writer.println("killall booksim");
        writer.println("killall vsim");
        writer.println("killall vsimk");
        writer.println();
        writer.println("rm -r work");
        writer.println("rm transcript");
        writer.println("rm socket");
        writer.println("rm *.out");
        writer.println("rm *.o *.so *.wlf *.vstf");

        ReportData.getInstance().closeQuickScriptFile();
    }
}
