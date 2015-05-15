package lynx.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lynx.data.MyEnums.Direction;
import lynx.data.MyEnums.PortType;

/**
 * A user-entered Module
 * 
 * @author Mohamed
 *
 */
public class DesignModule extends Module {

    private Map<String, Bundle> bundles;

    private int router;

    public DesignModule() {
        this(null, null);
    }

    public DesignModule(String type) {
        this(type, null);
    }

    public DesignModule(String type, String name) {
        this(type, name, -1);
    }

    public DesignModule(String type, String name, int router) {
        super(type, name);
        bundles = new HashMap<String, Bundle>();
        this.router = router;
    }

    public Map<String, Bundle> getBundles() {
        return bundles;
    }

    public Bundle getBundleByName(String bunName) {
        return bundles.get(bunName);
    }

    public void addBundle(Bundle bun) {
        bundles.put(bun.getName(), bun);
    }

    public int getRouter() {
        return router;
    }

    public void setRouter(int router) {
        this.router = router;
    }

    public List<String> getConnectedModuleNames(Direction dir) {
        List<String> mods = new ArrayList<String>();

        for (Bundle bun : bundles.values()) {
            if (bun.getDirection() == dir)
                for (Bundle conBun : bun.getConnections()) {
                    DesignModule conMod = conBun.getParentModule();
                    mods.add(conMod.getName());
                }
        }

        return mods;
    }

    public int getNumberOfConnections() {
        int numConnections = 0;
        for (Bundle bun : bundles.values()) {
            numConnections += bun.getConnections().size();
        }
        return numConnections;
    }

    public Port getClock() {
        for (Port por : getPorts().values()) {
            if (por.getType() == PortType.CLK)
                return por;
        }
        assert false : "module " + getName() + " has no clock?";
        return null;
    }

}
