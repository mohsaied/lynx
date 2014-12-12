package lynx.data;

import java.util.HashMap;
import java.util.Map;

/**
 * A user-entered Module
 * 
 * @author Mohamed
 *
 */
public class DesignModule extends Module<Port> {

    private Map<String, Bundle> bundles;

    public DesignModule() {
        this(null, null);
    }

    public DesignModule(String type) {
        this(type, null);
    }

    public DesignModule(String type, String name) {
        super(type, name);
        bundles = new HashMap<String, Bundle>();
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
}
