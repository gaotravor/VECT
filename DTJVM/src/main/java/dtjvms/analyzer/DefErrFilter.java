package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;

import java.util.*;
public class DefErrFilter extends Filter{

    private static final List<String> FILTER_STRINGS = new ArrayList<>();
    private static final HashMap<String, ArrayList<String>> DEF_EE_PAIR = new HashMap<>();
    private static final List<String> SIM_EXCEPTION_KEY = new ArrayList<>(); // 相似异常关键字

    private static final List<String> CLEAR_EXCEPTION_KEY = new ArrayList<>(); // 遇到直接认为不是差异

    static {
        FILTER_STRINGS.add("ExceptionInInitializerError");
        FILTER_STRINGS.add("ensureError");
        FILTER_STRINGS.add("recordInitializationFailure");

        DEF_EE_PAIR.put("1", new ArrayList<>(Arrays.asList("NegativeArraySizeException", "IllegalAccessError")));
        DEF_EE_PAIR.put("2", new ArrayList<>(Arrays.asList("VerifyError", "IllegalAccessError")));
//        DEF_EE_PAIR.put("2", new ArrayList<String>(Arrays.asList("ClassFormatError", "LinkageError")));

//        HOTSPOT_DEF_FP.add("VerifyError");
//        HOTSPOT_DEF_FP.add("IllegalMonitorStateException");
//        DEF_EE_PAIR.put("3", new ArrayList<String>(Arrays.asList("VerifyError", "RuntimeException")));
        //IllegalMonitorStateException

        SIM_EXCEPTION_KEY.add("IndexOutOfBounds".toLowerCase(Locale.ROOT));

//        CLEAR_EXCEPTION_KEY.add("OutOfMemoryError".toLowerCase(Locale.ROOT));
        CLEAR_EXCEPTION_KEY.add("JvmOutput-TIMEOUT".toLowerCase(Locale.ROOT));
//        CLEAR_EXCEPTION_KEY.add("Not enough space".toLowerCase(Locale.ROOT));

    }

    @Override
    protected void doFilter(HashMap<String, JvmOutput> results) {

        Set<String> allFEE = new HashSet<>();
        for (String key : results.keySet()) {

            JvmOutput jvmOut = results.get(key);

            allFEE.addAll(jvmOut.getFEEInfo());
            for (String filterString : FILTER_STRINGS) {

                jvmOut.getFailures().remove(filterString);
                jvmOut.getExceptions().remove(filterString);
                jvmOut.getErrors().remove(filterString);
            }
        }
        //EEF pair filter
        for (String s : DEF_EE_PAIR.keySet()) {

            ArrayList<String> defPair = DEF_EE_PAIR.get(s);
            if (equalCheck(defPair, allFEE)){

                for (String key : results.keySet()) {
                    JvmOutput jvmOut = results.get(key);
                    for (String s1 : defPair) {
                        jvmOut.getFailures().remove(s1);
                        jvmOut.getExceptions().remove(s1);
                        jvmOut.getErrors().remove(s1);
                    }
                }
            }
        }
        for (String exceptionKey : SIM_EXCEPTION_KEY) {
            boolean allExistFlag = true; // 记录是不是每一个jvmOut都包含异常关键词
            for (String key : results.keySet()) {
                JvmOutput jvmOut = results.get(key);
                boolean existFlag = false; // 记录当前jvmOut是否包含异常关键字
                for (String s : jvmOut.getFEEInfo()) {
                    if (s.toLowerCase(Locale.ROOT).contains(exceptionKey)) {
                        existFlag = true;
                        break;
                    }
                }
                if(!existFlag){
                    allExistFlag = false;
                }
            }
            if(allExistFlag){
                for (String key : results.keySet()) {
                    JvmOutput jvmOut = results.get(key);
                    jvmOut.getFailures().clear();
                    jvmOut.getExceptions().clear();
                    jvmOut.getErrors().clear();
                }
            }
        }

        boolean clearFlag = false;
        for (String exceptionKey : CLEAR_EXCEPTION_KEY) {
            for (String key : results.keySet()) {
                JvmOutput jvmOut = results.get(key);
                for (String s : jvmOut.getFEEInfo()) {
                    if (s.toLowerCase(Locale.ROOT).contains(exceptionKey)) {
                        clearFlag = true;
                        break;
                    }
                }
            }
        }
        if(clearFlag){
            for (String key : results.keySet()) {
                JvmOutput jvmOut = results.get(key);
                jvmOut.getFailures().clear();
                jvmOut.getExceptions().clear();
                jvmOut.getErrors().clear();
                jvmOut.setStdout("");
            }
        }



    }

    public boolean equalCheck(List<String> defPair, Set<String> currentFEE){

//        if (defPair.size() == currentFEE.size()){

        boolean isSame = true;
        for (String s : defPair) {

            if (!currentFEE.contains(s)){
                isSame = false;
                break;
            }
        }
        return isSame;
//        } else {
//            return false;
//        }
    }
}