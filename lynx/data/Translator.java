package lynx.data;

import lynx.data.MyEnums.Direction;

public class Translator extends Module {

    private Module parentModule;
    private Bundle parentBundle;
    private Direction direction;

    public Translator() {
        this(null, null, null, null);
    }

    public Translator(String type) {
        this(type, null, null, null);
    }

    public Translator(String type, String name) {
        this(type, name, null, null);
    }

    public Translator(String type, String name, Module parentModule, Bundle parentBundle) {
        super(type, name);
        this.parentModule = parentModule;
        this.parentBundle = parentBundle;
        this.direction = parentBundle.getDirection();
    }

    public Module getParentModule() {
        return parentModule;
    }

    public void setParentModule(Module parentModule) {
        this.parentModule = parentModule;
    }

    public Bundle getParentBundle() {
        return parentBundle;
    }

    public void setParentBundle(Bundle parentBundle) {
        this.parentBundle = parentBundle;
        this.direction = parentBundle.getDirection();
    }

    public Direction getDirection() {
        return direction;
    }

}
