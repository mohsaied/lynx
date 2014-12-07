package lynx.log;

import java.util.logging.Level;
import java.util.logging.Logger;

import lynx.data.Design;
import lynx.data.Module;
import lynx.main.Main;
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
            Module.class.getName() };

    public MyLogger() {

        for (String currClass : classes) {
            Logger log = Logger.getLogger(currClass);
            log.setLevel(Level.ALL);
        }

    }

    public MyLogger(Level logLevel) {

        for (String currClass : classes) {
            Logger log = Logger.getLogger(currClass);
            log.setLevel(logLevel);
        }

    }
}
