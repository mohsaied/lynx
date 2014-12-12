package lynx.data;

import java.util.HashMap;
import java.util.Map;

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
}
