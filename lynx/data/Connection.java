package lynx.data;

/**
 * Connection exists between two modules
 * 
 * @author Mohamed
 *
 */
public class Connection {

    private Bundle fromBundle;
    private Bundle toBundle;

    @SuppressWarnings("unused")
    private Design design;

    private DesignModule fromModule;
    private DesignModule toModule;

    private int fromModuleIndex;
    private int toModuleIndex;

    private int latencySpec;

    public Connection(Bundle fromBundle, Bundle toBundle, Design design) {

        this.design = design;

        this.fromBundle = fromBundle;
        this.toBundle = toBundle;

        this.fromModule = fromBundle.getParentModule();
        this.toModule = toBundle.getParentModule();

        this.fromModuleIndex = design.getModuleIndex(fromModule.getName());
        this.toModuleIndex = design.getModuleIndex(toModule.getName());

        this.setLatencySpec(1); // default latency requirement
    }

    public final Bundle getFromBundle() {
        return fromBundle;
    }

    public final void setFromBundle(Bundle fromBundle) {
        this.fromBundle = fromBundle;
    }

    public final Bundle getToBundle() {
        return toBundle;
    }

    public final void setToBundle(Bundle toBundle) {
        this.toBundle = toBundle;
    }

    public final DesignModule getFromModule() {
        return fromModule;
    }

    public final void setFromModule(DesignModule fromModule) {
        this.fromModule = fromModule;
    }

    public final DesignModule getToModule() {
        return toModule;
    }

    public final void setToModule(DesignModule toModule) {
        this.toModule = toModule;
    }

    public final int getFromModuleIndex() {
        return fromModuleIndex;
    }

    public final void setFromModuleIndex(int fromModuleIndex) {
        this.fromModuleIndex = fromModuleIndex;
    }

    public final int getToModuleIndex() {
        return toModuleIndex;
    }

    public final void setToModuleIndex(int toModuleIndex) {
        this.toModuleIndex = toModuleIndex;
    }

    public int getLatencySpec() {
        return latencySpec;
    }

    public void setLatencySpec(int latency_spec) {
        this.latencySpec = latency_spec;
    }

    @Override
    public String toString() {
        return fromBundle.getFullName() + "-->" + toBundle.getFullName();

    }

}
