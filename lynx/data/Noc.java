package lynx.data;

/**
 * NoC class is a special module that instantiates an NoC interface (a.k.a.
 * fabricinterface)
 * 
 * @author Mohamed
 *
 */
public class Noc extends Module {

    public Noc() {
        super("fabric_interface", "fi_inst");
    }

}
