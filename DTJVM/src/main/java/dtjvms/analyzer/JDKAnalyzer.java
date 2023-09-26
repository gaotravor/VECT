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

        filterChain.startFilter(results);


        return checkIfOutputConsistentWithDiffCore(results);
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

