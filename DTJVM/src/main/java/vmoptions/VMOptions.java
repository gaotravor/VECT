package vmoptions;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VMOptions {

    private String vmname;
    private List<Option> options;

    public VMOptions(String name) {
        this.vmname = name;
        options = new ArrayList<>();
    }

    public void addOption(Option option){
        options.add(option);
    }

    public String getVmname() {
        return vmname;
    }

    public void setVmname(String vmname) {
        this.vmname = vmname;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }
}
