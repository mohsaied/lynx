package lynx.data;

/**
 * NoC class is a special module that instantiates an NoC interface (a.k.a.
 * fabricinterface)
 * 
 * @author Mohamed
 *
 */
public class Noc extends Module {

    private static final String nocName = "fabric_interface";
    private static final String nocInstName = "fi_inst";

    public Noc() {
        super(nocName, nocInstName);
    }

}
