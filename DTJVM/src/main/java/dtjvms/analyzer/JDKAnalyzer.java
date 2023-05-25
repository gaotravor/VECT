package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;

import java.util.*;

public class JDKAnalyzer extends Analyzer{

    public static JDKAnalyzer jdkAnalyzer;
    public static int RESULT_LEVEL1 = -1;
    public static int RESULT_LEVEL2 = 0;
    public static int RESULT_LEVEL3 = 1;

    public boolean discardFlag = false;
    public int resultState = 0;

    public static JDKAnalyzer getInstance(){

        if (jdkAnalyzer == null){
            jdkAnalyzer = new JDKAnalyzer();
        }
        return jdkAnalyzer;
    }

    /**
     * Return value state:
     *          -1  Exception and consistent
     *           0  Normal execute without exception
     *           1  Difference found (both exception and normal execute)
     * @param results
     * @return
     */
    @Override
    public DiffCore analysis(String className, HashMap<String, JvmOutput> results){

        FilterChain filterChain = new FilterChain();

        filterChain.addFilter(new JunitFilter());

        filterChain.addFilter(new StdErrFilter());

        filterChain.addFilter(new DefErrFilter());

        filterChain.addFilter(new HotSpotDefErrFilter());


//        filterChain.addFilter(new OpenJ9DefErrFilter());
        filterChain.startFilter(results);


        return checkIfOutputConsistentWithDiffCore(results);
//        return checkIfOldConsistentWithNew(results);
    }
    public DiffCore checkIfOldConsistentWithNew(HashMap<String, JvmOutput> results){

        DiffCore diffCore = null;
        discardFlag = false;

        JvmOutput hotspot_old = null;
        JvmOutput hotspot_new = null;
        JvmOutput openj9_old = null;
        JvmOutput openj9_new = null;
        JvmOutput bisheng_old = null;
        JvmOutput bisheng_new = null;

        for (String s : results.keySet()) {
            boolean old = s.toLowerCase().contains("_old") || s.toLowerCase().contains("-old");
            if (s.toLowerCase().contains("hotspot")){

                if (old){
                    hotspot_old = results.get(s);
                } else {
                    hotspot_new = results.get(s);
                }
            } else if (s.toLowerCase().contains("openj9")){
                if (old){
                    openj9_old = results.get(s);
                } else {
                    openj9_new = results.get(s);
                }
            } else if(s.toLowerCase().contains("bisheng")){
                if (old){
                    bisheng_old = results.get(s);
                } else {
                    bisheng_new = results.get(s);
                }
            }else {

            }
        }
 
        if (!exitValueConsistent(results)){
            discardFlag = true;
        }
        if (bisheng_old != null){
            //01 check if three old version is consistent
            boolean old_consistent = checkIfThreeOutputConsistent(hotspot_old,openj9_old,bisheng_old);
            boolean new_consistent = checkIfThreeOutputConsistent(hotspot_new,openj9_new,bisheng_new);
            if(!old_consistent){
                if(new_consistent){
                    //bug found
                    diffCore = new DiffCore(0,discardFlag,"Old InConsistent, New Consistent");
                    diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                    return diffCore;
                }else {
                    //check old and new
                    boolean hospot_consistent = checkIfTwoOutputConsistent(hotspot_old, hotspot_new);
                    boolean openj9_consistent = checkIfTwoOutputConsistent(openj9_old, openj9_new);
                    boolean bisheng_consistent = checkIfTwoOutputConsistent(bisheng_old,bisheng_new);
                    if (hospot_consistent && openj9_consistent && bisheng_consistent){
                        return null;
                    }else {
                        if (!hospot_consistent && !openj9_consistent && !bisheng_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"For rules InConsistent");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                        if (!hospot_consistent && !openj9_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Hotspot and Openj9");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                        if (!openj9_consistent && !bisheng_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Openj9 and Bisheng");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                        if (!hospot_consistent && !bisheng_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Hotspot and Bisheng");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                        if (!hospot_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Hotspot");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                        if (!openj9_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Openj9");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                        if (!bisheng_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Bisheng");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new, bisheng_old, bisheng_new));
                            return diffCore;
                        }
                    }
                }
            }
        }else {
            //01 check if two old version is consistent
            boolean old_consistent = checkIfTwoOutputConsistent(hotspot_old, openj9_old);
            boolean new_consistent = checkIfTwoOutputConsistent(hotspot_new, openj9_new);

            if (!old_consistent) {

                if (new_consistent){

                    //bug found
                    diffCore = new DiffCore(0,discardFlag,"Old InConsistent, New Consistent");
                    diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new));
                    return diffCore;
                } else {

                    //check old and new
                    boolean hospot_consistent = checkIfTwoOutputConsistent(hotspot_old, hotspot_new);
                    boolean openj9_consistent = checkIfTwoOutputConsistent(openj9_old, openj9_new);

                    if (hospot_consistent && openj9_consistent){
                        return null;
                    } else {

                        if (!hospot_consistent && !openj9_consistent) {

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"For rules InConsistent");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new));
                            return diffCore;
                        }

                        if (!hospot_consistent){

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Hotspot");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new));
                            return diffCore;
                        }

                        if (!openj9_consistent){

                            //bug found
                            diffCore = new DiffCore(0,discardFlag,"Old InConsistent, Target Openj9");
                            diffCore.setDetailedMessage(generateDetailedMessage(hotspot_old, hotspot_new, openj9_old, openj9_new));
                            return diffCore;
                        }
                    }
                }
            }
            return null;
        }
        return  null;
    }
    public String generateDetailedMessage(JvmOutput hotspot_old, JvmOutput hotspot,
                                          JvmOutput openj9_old, JvmOutput openj9,
                                          JvmOutput bisheng_old,JvmOutput bisheng){
        List<String> list;
        String detailedMessage = "hotspot_old FIRST hotspot SECOND openj9_old THIRD openj9 FORTH bisheng_old FIFTH bisheng SIXTH";

        list = hotspot_old.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("FIRST", list.toString());

        list = hotspot.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("SECOND", list.toString());

        list = openj9_old.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("THIRD", list.toString());

        list = openj9.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("FORTH", list.toString());

        list = bisheng_old.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("FIFTH", list.toString());

        list = bisheng.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("SIXTH", list.toString());

        return detailedMessage;
    }
    public String generateDetailedMessage(JvmOutput hotspot_old, JvmOutput hotspot,
                                          JvmOutput openj9_old, JvmOutput openj9){
        List<String> list;
        String detailedMessage = "hotspot_old FIRST hotspot SECOND openj9_old THIRD openj9 FORTH";

        list = hotspot_old.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("FIRST", list.toString());

        list = hotspot.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("SECOND", list.toString());

        list = openj9_old.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("THIRD", list.toString());

        list = openj9.getFEEInfo();
        Collections.sort(list);
        detailedMessage = detailedMessage.replace("FORTH", list.toString());
        return detailedMessage;
    }

    public boolean checkIfThreeOutputConsistent(JvmOutput jvm1,JvmOutput jvm2,JvmOutput jvm3){
        // 01 Errors
        if(!checkThreeNumber(jvm1.getErrors().size(),jvm2.getErrors().size(),jvm3.getErrors().size())){
            return false;
        }else {
            Set<String> tmpSet = new HashSet<>();
            tmpSet.addAll(jvm1.getErrors());
            tmpSet.addAll(jvm2.getErrors());
            tmpSet.addAll(jvm3.getErrors());
            if (tmpSet.size() != jvm1.getErrors().size()){
                return false;
            }
        }
        // 02 Exceptions
        if(!checkThreeNumber(jvm1.getExceptions().size(),jvm2.getExceptions().size(),jvm3.getExceptions().size())){
            return false;
        }else {
            Set<String> tmpSet = new HashSet<>();
            tmpSet.addAll(jvm1.getExceptions());
            tmpSet.addAll(jvm2.getExceptions());
            tmpSet.addAll(jvm3.getExceptions());
            if (tmpSet.size() != jvm1.getExceptions().size()){
                return false;
            }
        }
        //03 Failures
        if(!checkThreeNumber(jvm1.getFailures().size(),jvm2.getFailures().size(),jvm3.getFailures().size())){
            return false;
        }else {
            Set<String> tmpSet = new HashSet<>();
            tmpSet.addAll(jvm1.getFailures());
            tmpSet.addAll(jvm2.getFailures());
            tmpSet.addAll(jvm3.getFailures());
            if (tmpSet.size() != jvm1.getFailures().size()){
                return false;
            }
        }
        // 04 StdOut
        if(!jvm1.getStdout().equals(jvm2.getStdout())){
            return false;
        }
        if(!jvm2.getStdout().equals(jvm3.getStdout())){
            return false;
        }
        if(!jvm1.getStdout().equals(jvm3.getStdout())){
            return false;
        }
        return true;
    }
    private boolean checkThreeNumber(int i1,int i2,int i3){
        if(i1 != i2){
            return false;
        }
        return i2 == i3;
    }
    public boolean checkIfTwoOutputConsistent(JvmOutput jvm1, JvmOutput jvm2){

        //01 Errors
        if (jvm1.getErrors().size() != jvm2.getErrors().size()){

            return false;
        } else {

            Set<String> tmpSet = new HashSet<>();
            tmpSet.addAll(jvm1.getErrors());
            tmpSet.addAll(jvm2.getErrors());
            if (tmpSet.size() != jvm1.getErrors().size()){

                return false;
            }
        }

        //02 Exceptions
        if (jvm1.getExceptions().size() != jvm2.getExceptions().size()){

            return false;
        } else {

            Set<String> tmpSet = new HashSet<>();
            tmpSet.addAll(jvm1.getExceptions());
            tmpSet.addAll(jvm2.getExceptions());
            if (tmpSet.size() != jvm1.getExceptions().size()){

                return false;
            }
        }

        //03 Failures
        if (jvm1.getFailures().size() != jvm2.getFailures().size()){

            return false;
        } else {

            Set<String> tmpSet = new HashSet<>();
            tmpSet.addAll(jvm1.getFailures());
            tmpSet.addAll(jvm2.getFailures());
            if (tmpSet.size() != jvm1.getFailures().size()){

                return false;
            }
        }
        //04 StdOut
        if(!jvm1.getStdout().equals(jvm2.getStdout())){
            return false;
        }
        return true;
    }
    public boolean getDiscardFlag() {
        return discardFlag;
    }

    public void setDiscardFlag(boolean discardFlag) {
        this.discardFlag = discardFlag;
    }

    public int getResultState() {
        return resultState;
    }

    public static boolean exitValueConsistent(HashMap<String, JvmOutput> results){

        int normalExecCount = 0;
        for (Map.Entry<String, JvmOutput> result : results.entrySet()) {
            if (result.getValue().getExitValue() == 0){
                normalExecCount++;
            }
        }
        if (normalExecCount < results.keySet().size()){
            return false;
        } else if (normalExecCount == results.keySet().size()){
            return true;
        } else {
            throw new RuntimeException("This should not happen: normalExecCount (" +
                    normalExecCount +
                    ") is greater than results size (" +
                    results.keySet().size() +
                    ")");
        }
    }

    public DiffCore checkIfOutputConsistentWithDiffCore(HashMap<String, JvmOutput> results){

        DiffCore diffCore = null;
        String DetailedMessage = genDetailedMessage(results);
        discardFlag = false;
        if (!exitValueConsistent(results)){

            discardFlag = true;
            //Error, Exception, Failure
            ArrayList<String> keys = new ArrayList<>(results.keySet());
            for (int i = 0; i < keys.size(); i++) {

                JvmOutput object1 = results.get(keys.get(i));
                for (int j = i; j < keys.size(); j++){

                    JvmOutput object2 = results.get(keys.get(j));

                    //01 Errors
                    if (object1.getErrors().size() != object2.getErrors().size()){

                        resultState = RESULT_LEVEL3;
                        diffCore = new DiffCore(0,discardFlag,"Error Size Inconsistent");
                        diffCore.setDetailedMessage(DetailedMessage);
                        return diffCore;
                    } else {

                        Set<String> tmpSet = new HashSet<>();
                        tmpSet.addAll(object1.getErrors());
                        tmpSet.addAll(object2.getErrors());
                        if (tmpSet.size() != object1.getErrors().size()){
                            resultState = RESULT_LEVEL3;
                            diffCore = new DiffCore(0,discardFlag,"Error Inconsistent");
                            diffCore.setDetailedMessage(DetailedMessage);
                            return diffCore;
                        }
                    }

                    //02 Exceptions
                    if (object1.getExceptions().size() != object2.getExceptions().size()){
                        resultState = RESULT_LEVEL3;
                        diffCore = new DiffCore(1,discardFlag,"Exception Inconsistent");
                        diffCore.setDetailedMessage(DetailedMessage);
                        return diffCore;
                    } else {

                        Set<String> tmpSet = new HashSet<>();
                        tmpSet.addAll(object1.getExceptions());
                        tmpSet.addAll(object2.getExceptions());
                        if (tmpSet.size() != object1.getExceptions().size()){
                            resultState = RESULT_LEVEL3;
                            diffCore = new DiffCore(1,discardFlag,"Exception Inconsistent");
                            diffCore.setDetailedMessage(DetailedMessage);
                            return diffCore;
                        }
                    }

                    //03 Failures
                    if (object1.getFailures().size() != object2.getFailures().size()){
                        resultState = RESULT_LEVEL3;
                        diffCore = new DiffCore(2,discardFlag,"Failure Inconsistent");
                        diffCore.setDetailedMessage(DetailedMessage);
                        return diffCore;
                    } else {

                        Set<String> tmpSet = new HashSet<>();
                        tmpSet.addAll(object1.getFailures());
                        tmpSet.addAll(object2.getFailures());
                        if (tmpSet.size() != object1.getFailures().size()){
                            resultState = RESULT_LEVEL3;
                            diffCore = new DiffCore(2,discardFlag,"Failure Inconsistent");
                            diffCore.setDetailedMessage(DetailedMessage);
                            return diffCore;
                        }
                    }
                }
            }
            resultState = RESULT_LEVEL1;
        } else {
//            //Normal
            ArrayList<String> keys = new ArrayList<>(results.keySet());
            for (int i = 0; i < keys.size(); i++) {

                JvmOutput object1 = results.get(keys.get(i));
                for (int j = i; j < keys.size(); j++){

                    JvmOutput object2 = results.get(keys.get(j));

                    if (!object1.getStdout().equals(object2.getStdout())){

                        diffCore = new DiffCore(3,discardFlag,"Normal Output Inconsistent");
                        diffCore.setDetailedMessage(DetailedMessage);
                        return diffCore;
                    }
                }
            }
            return null;
        }
        return null;
    }
    private static String genDetailedMessage(HashMap<String, JvmOutput> results){
        List<String> jvmIdList = new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : results.keySet()) {
            jvmIdList.add(s);
        }
        Collections.sort(jvmIdList);

        String detailedMessage = "";
        for(int i =0;i<jvmIdList.size();i++){
            detailedMessage += jvmIdList.get(i);
            detailedMessage += " ";
            detailedMessage += ("placeholder"+i);
            detailedMessage += " ";
        }
        boolean isCrash = false;
        for(int i=0;i< jvmIdList.size();i++){
            JvmOutput jvmOutput =  results.get(jvmIdList.get(i));
            list = jvmOutput.getFEEInfo();
            if (list.size() == 0){
//                detailedMessage = detailedMessage.replace("placeholder"+i,"[]");
//                detailedMessage = detailedMessage.replace("placeholder"+i,"["+jvmOutput.getStdout().replace("\n","").replace("my_check_sum_value:","")+"]");
                continue;
            }
            isCrash = true;
            Collections.sort(list);
            detailedMessage = detailedMessage.replace("placeholder"+i,list.toString());
        }
        if(isCrash){
            for(int i=0;i< jvmIdList.size();i++){
                if(detailedMessage.contains("placeholder"+i)){
                    detailedMessage = detailedMessage.replace("placeholder"+i,"[]");
                }
            }
        }else {
            for(int i=0;i< jvmIdList.size();i++){
                JvmOutput jvmOutput =  results.get(jvmIdList.get(i));
                String checkSum = "";
                try {
                    checkSum = jvmOutput.getStdout().replace("\n","").replace("my_check_sum_value:","");
                }catch (Exception e){
                    checkSum = "unknown";
                }
                detailedMessage = detailedMessage.replace("placeholder"+i,"["+checkSum+"]");
            }
        }
        return detailedMessage;
    }
}

