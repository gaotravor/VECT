package vmoptions;

import dtjvms.DTConfiguration;
import java.util.ArrayList;
import java.util.Locale;

public class GenVMOptions {

    public static VMOptions genVMOptions(String jvmName,String version){
        if(jvmName.toLowerCase(Locale.ROOT).contains("hotspot")){
            if(version.equals("openjdk8")){
                return genHotspot8VMOptions(jvmName);
            }
            if(version.equals("openjdk11")){
                return genHotspot11VMOptions(jvmName);
            }
        }
        if(jvmName.toLowerCase(Locale.ROOT).contains("openj9")){
            if(version.equals("openjdk8")){
                return null;
            }
            if(version.equals("openjdk11")){
                return null;
            }
        }
        if(jvmName.toLowerCase(Locale.ROOT).contains("bisheng")){
            if(version.equals("openjdk8")){
                return null;
            }
            if(version.equals("openjdk11")){
                return null;
            }
        }
        return null;
    }
    private static VMOptions genHotspot8VMOptions(String jvmName){
        VMOptions vmOptions = new VMOptions(jvmName);
        ArrayList<Option> options = new ArrayList<>();
        int MAX_INTX = 2147483647;
        int MEMORY_RELATED = 1048575;
        options.add(new NumericOption("ArrayOperationPartialInlineSize",0 ,64 ));
        options.add(new NumericOption("AutoBoxCacheMax",0 ,MEMORY_RELATED ));
        options.add(new NumericOption("BlockLayoutMinDiamondPercentage",0 ,100 ));
        options.add(new NumericOption("LiveNodeCountInliningCutoff",0 , 536870911));
        options.add(new NumericOption("LoopOptsCount",5 ,43 ));
        options.add(new NumericOption("LoopPercentProfileLimit", 10,100 ));
        options.add(new NumericOption("NestedInliningSizeRatio",0 ,100 ));
        options.add(new NumericOption("OptoPrologueNops",0 ,128 ));
        options.add(new NumericOption("TrackedInitializationLimit",0 ,65535 ));
        options.add(new NumericOption("TypeProfileMajorReceiverPercent",0 , 100));
        options.add(new NumericOption("ValueMapMaxLoopSize",0 ,128 ));
        for (String s : DTConfiguration.HOTSPOT_OPTIONS_NUMERIC) {
            options.add(new NumericOption(s,0 ,MAX_INTX ));
        }
        for (String s : DTConfiguration.HOTSPOT8_OPTIONS_BOOLEAN) {
            options.add(new BooleanOption(s));
        }
        vmOptions.setOptions(options);
        return vmOptions;
    }
    private static VMOptions genHotspot11VMOptions(String jvmName){
        VMOptions vmOptions = new VMOptions(jvmName);
        ArrayList<Option> options = new ArrayList<>();
        int MAX_INTX = 2147483647;
        int MEMORY_RELATED = 1048575;
        options.add(new NumericOption("ArrayOperationPartialInlineSize",0 ,64 ));
        options.add(new NumericOption("AutoBoxCacheMax",0 ,MEMORY_RELATED ));
        options.add(new NumericOption("BlockLayoutMinDiamondPercentage",0 ,100 ));
        options.add(new NumericOption("LiveNodeCountInliningCutoff",0 , 536870911));
        options.add(new NumericOption("LoopOptsCount",5 ,43 ));
        options.add(new NumericOption("LoopPercentProfileLimit", 10,100 ));
        options.add(new NumericOption("NestedInliningSizeRatio",0 ,100 ));
        options.add(new NumericOption("OptoPrologueNops",0 ,128 ));
        options.add(new NumericOption("TrackedInitializationLimit",0 ,65535 ));
        options.add(new NumericOption("TypeProfileMajorReceiverPercent",0 , 100));
        options.add(new NumericOption("ValueMapMaxLoopSize",0 ,128 ));
        for (String s : DTConfiguration.HOTSPOT_OPTIONS_NUMERIC) {
            options.add(new NumericOption(s,0 ,MAX_INTX ));
        }
        for (String s : DTConfiguration.HOTSPOT11_OPTIONS_BOOLEAN) {
            options.add(new BooleanOption(s));
        }
        vmOptions.setOptions(options);
        return vmOptions;
    }
}
