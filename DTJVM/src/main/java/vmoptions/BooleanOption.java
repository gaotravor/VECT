package vmoptions;

public class BooleanOption extends Option{

    public BooleanOption(String name) {
        super(name);
    }

    @Override
    public String getCmd(){
        setLastCmd("-XX:+"+getName());
        return  "-XX:+"+getName();
    }
}
