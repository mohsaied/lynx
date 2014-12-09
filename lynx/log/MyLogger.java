package lynx.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import lynx.Interconnect.NocInterconnect;
import lynx.data.Design;
import lynx.data.Module;
import lynx.main.Main;
import lynx.verilog.VerilogOut;
import lynx.xml.XmlDesign;
import lynx.xml.XmlNoc;

/**
 * Groups and keeps track of loggers
 * 
 * @author Mohamed
 *
 */
public class MyLogger {

    // classes that contain loggers
    private final String[] classes = { Main.class.getName(), XmlDesign.class.getName(), Design.class.getName(),
            Module.class.getName(), VerilogOut.class.getName(), NocInterconnect.class.getName(), XmlNoc.class.getName() };

    public MyLogger() {

        this(Level.ALL);

    }

    public MyLogger(Level logLevel) {

        MyFormatter formatter = new MyFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);

        for (String currClass : classes) {
            Logger log = Logger.getLogger(currClass);

            log.setUseParentHandlers(false);

            log.addHandler(handler);

            log.setLevel(logLevel);
        }

    }
}
