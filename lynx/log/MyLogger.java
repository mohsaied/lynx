package lynx.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Module;
import lynx.main.Main;
import lynx.noc.Interconnect;
import lynx.verilog.VerilogOut;
import lynx.xml.XMLIO;

/**
 * Groups and keeps track of loggers
 * 
 * @author Mohamed
 *
 */
public class MyLogger {

    // classes that contain loggers
    private final String[] classes = { Main.class.getName(), XMLIO.class.getName(), Design.class.getName(),
            Module.class.getName(), VerilogOut.class.getName(), Interconnect.class.getName() };

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
