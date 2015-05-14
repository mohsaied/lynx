package lynx.hdlgen;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import lynx.data.Noc;
import lynx.main.ReportData;

/**
 * Output the noc_config file that goes into rtl2booksim
 * 
 * @author Mohamed
 *
 */
public class NocConfigOut {

    private static final Logger log = Logger.getLogger(NocConfigOut.class.getName());

    public static void writeNocConfig(Noc noc) throws FileNotFoundException, UnsupportedEncodingException {

        PrintWriter writer = ReportData.getInstance().getNocConfigFile();

        log.info("Generating noc_config file");

        int k = (int) Math.sqrt(noc.getNumRouters());

        writer.println("topology = mesh;");
        writer.println("k = " + k + ";");
        writer.println("n = 2;");
        writer.println();
        writer.println("flit_width = " + noc.getWidth() + ";");
        writer.println();
        writer.println("routing_function = dim_order;");
        writer.println();
        writer.println("// Flow control");
        writer.println("num_vcs     = 2;");
        writer.println("vc_buf_size = 16;");
        writer.println("wait_for_tail_credit = 0;");
        writer.println();
        writer.println("read_request_begin_vc = 0;");
        writer.println("read_request_end_vc = 0;");
        writer.println();
        writer.println("read_reply_begin_vc = 1;");
        writer.println("read_reply_end_vc = 1;");
        writer.println();
        writer.println("write_request_begin_vc = 2;");
        writer.println("write_request_end_vc = 2;");
        writer.println();
        writer.println("write_reply_begin_vc = 3;");
        writer.println("write_reply_end_vc = 3;");
        writer.println();
        writer.println("// Router architecture");
        writer.println("routing_delay  = 0;");
        writer.println("speculative = 1;");
        writer.println();
        writer.println("latency_thres = 10000.0;");
        writer.println();
        writer.println("// Simulation");
        writer.println("sim_type = fes2;");

        ReportData.getInstance().closeNocConfigFile();
    }

}
