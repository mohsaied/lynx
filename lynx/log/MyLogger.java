package lynx.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates the parent logger and formats it properly
 * 
 * @author Mohamed
 *
 */
public class MyLogger {

    private Logger mainLog;

    public MyLogger() {

        this(Level.ALL);
    }

    public MyLogger(Level logLevel) {

        MyFormatter formatter = new MyFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);

        // only need to create a parent logger with the parent namespace (in my
        // case that's "lynx"), and any child automatically inherits all of its
        // properties upon creation
        mainLog = Logger.getLogger("lynx");
        mainLog.setUseParentHandlers(false);
        mainLog.addHandler(handler);
        mainLog.setLevel(logLevel);
    }
}
