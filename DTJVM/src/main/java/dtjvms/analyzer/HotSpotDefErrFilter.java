package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HotSpotDefErrFilter extends Filter{

    //三种情况
    //1、Hotspot独有
    //2、所有都有，但hotspot会多输出其他的
    //3、
    private static final List<String> HOTSPOT_UNIQUE = new ArrayList<>();
    private static final List<String> HOTSPOT_DEF_FP = new ArrayList<>();

    static {

        HOTSPOT_UNIQUE.add("IllegalMonitorStateException");
        HOTSPOT_UNIQUE.add("IncompatibleClassChangeError");

        HOTSPOT_DEF_FP.add("VerifyError");
        HOTSPOT_DEF_FP.add("ClassFormatError");
    }

    @Override
    protected void doFilter(HashMap<String, JvmOutput> results) {

        filterHotspotUnique(results);
        filterDefFP(results);
    }

    public void filterDefFP(HashMap<String, JvmOutput> results){

        for (String s : HOTSPOT_DEF_FP) {

            if (checkIfAllContainsTargetErr(results, s)){

                for (String key : results.keySet()) {

//                    if (!key.toLowerCase().contains("openj9")){
                    JvmOutput jvmOut = results.get(key);
                    if (jvmOut.getErrors().contains(s)){
                        jvmOut.getErrors().clear();
                        jvmOut.getExceptions().clear();
                        jvmOut.getFailures().clear();
                        jvmOut.getErrors().add(s);
                    }
                    if (jvmOut.getExceptions().contains(s)){
                        jvmOut.getErrors().clear();
                        jvmOut.getExceptions().clear();
                        jvmOut.getFailures().clear();
                        jvmOut.getExceptions().add(s);
                    }
                    if (jvmOut.getFailures().contains(s)){
                        jvmOut.getErrors().clear();
                        jvmOut.getExceptions().clear();
                        jvmOut.getFailures().clear();
                        jvmOut.getFailures().add(s);
                    }

//                    }
                }
            }
        }
    }

    public boolean checkIfAllContainsTargetErr(HashMap<String, JvmOutput> results, String err){

        for (String key : results.keySet()) {

            JvmOutput jvmOut = results.get(key);
            if (!jvmOut.getFEEInfo().contains(err)){

                return false;
            }
        }
        return true;
    }

    public void filterHotspotUnique(HashMap<String, JvmOutput> results){

        boolean uniqueFlag = true;
        for (String s : HOTSPOT_UNIQUE) {

            uniqueFlag = true;
            for (String key : results.keySet()) {

                if (key.toLowerCase().contains("openj9")){
                    JvmOutput jvmOut = results.get(key);
                    if (jvmOut.getFEEInfo().contains(s)){
                        uniqueFlag = false;
                        break;
                    }
                }
            }
            if (uniqueFlag){
                for (String key : results.keySet()) {

                    if (!key.toLowerCase().contains("openj9")){

                        JvmOutput jvmOut = results.get(key);
                        jvmOut.getErrors().remove(s);
                        jvmOut.getExceptions().remove(s);
                        jvmOut.getFailures().remove(s);
                    }
                }
            }
        }
    }
}
