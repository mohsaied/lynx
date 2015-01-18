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
    
    public Connection(Bundle fromBundle, Bundle toBundle, Design design) {
        
        this.design = design;
        
        this.fromModule = fromBundle.getParentModule();
        this.toModule = toBundle.getParentModule();

        this.fromModuleIndex = design.getModuleIndex(fromModule.getName());
        this.toModuleIndex = design.getModuleIndex(toModule.getName());
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

}
