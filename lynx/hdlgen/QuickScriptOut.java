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

        log.info("Generating simulation quick_script");

        writer.println("not done yet");

        ReportData.getInstance().closeQuickScriptFile();
    }
}
