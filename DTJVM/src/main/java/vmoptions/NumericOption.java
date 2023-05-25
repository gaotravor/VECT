package vmoptions;

import java.util.Random;

public class NumericOption extends Option{

    private int max;
    private int min;
    public NumericOption(String name, int min, int max) {
        super(name);
        this.max = max;
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    @Override
    public String getCmd() {
        boolean flag = new Random().nextBoolean();
        int numeric = 0;
        if(flag){
            numeric = max;
        }else {
            numeric = min;
        }
        setLastCmd("-XX:"+getName()+"="+numeric);
        return "-XX:"+getName()+"="+numeric;
    }

}
