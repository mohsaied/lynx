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

    private static final int defaultNocWidth = 150;
    private static final int defaultNocNumRouters = 16;
    private static final int defaultNocNumVcs = 2;
    private static final int defaultNocVcDepth = 16;
    
    private int nocWidth;
    private int nocNumRouters;
    private int nocNumVcs;
    private int nocVcDepth;

    public Noc() {
        super(nocName, nocInstName);
        configureNoC(defaultNocWidth,defaultNocNumRouters,defaultNocNumVcs,defaultNocVcDepth);
    }
    
    public Noc(String nocPath){
        
    }

    public void configureNoC(int nocWidth, int nocNumRouters, int nocNumVcs, int nocVcDepth) {
        // TODO Auto-generated method stub

    }

}
