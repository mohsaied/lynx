package lynx.data;

import java.util.ArrayList;
import java.util.List;


/**
 * A user-entered Module
 * 
 * @author Mohamed
 *
 */
public class DesignModule extends Module {

    private List<Bundle> bundles;

    public DesignModule() {
        this(null, null);
    }

    public DesignModule(String type) {
        this(type, null);
    }

    public DesignModule(String type, String name) {
        super(type, name);
        bundles = new ArrayList<Bundle>();
    }

    public List<Bundle> getBundles() {
        return bundles;
    }

    public void addBundle(Bundle bun) {
        bundles.add(bun);
    }
}
