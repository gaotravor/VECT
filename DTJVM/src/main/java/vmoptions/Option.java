package vmoptions;

import java.util.HashSet;
import java.util.Set;

public class Option {

    private String name;
    private String lastCmd;

    public Option(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCmd(){
        return  null;
    }

    public String getLastCmd() {
        return lastCmd;
    }

    public void setLastCmd(String lastCmd) {
        this.lastCmd = lastCmd;
    }
}
