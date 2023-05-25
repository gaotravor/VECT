package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;

import java.util.ArrayList;
import java.util.HashMap;

public class OpenJ9DefErrFilter extends Filter{


    @Override
    protected void doFilter(HashMap<String, JvmOutput> results) {

        ArrayList<JvmOutput> others = new ArrayList<>();
        ArrayList<JvmOutput> j9s = new ArrayList<>();
        ArrayList<JvmOutput> j9xfuture = new ArrayList<>();
        for (String key : results.keySet()) {

            JvmOutput jvmOutput = results.get(key);
            if (key.toLowerCase().contains("openj9")){

                if (key.toLowerCase().contains("xfuture")){

                    j9xfuture.add(jvmOutput);
                } else {
                    j9s.add(jvmOutput);
                }
            } else {

                others.add(jvmOutput);
            }
        }

        if (!ifJ9Inconsist(j9s, j9xfuture)){

            if (ifJ9HotspotConsist(j9xfuture, others)){

                for (String key : results.keySet()) {

                    JvmOutput jvmOutput = results.get(key);
                    jvmOutput.getErrors().clear();
                    jvmOutput.getExceptions().clear();
                    jvmOutput.getFailures().clear();
                }
            }
        }
    }

    public boolean ifJ9Inconsist(ArrayList<JvmOutput> j9s, ArrayList<JvmOutput> j9xfuture){

        for (JvmOutput j9 : j9s) {

            for (JvmOutput jvmOutput : j9xfuture) {

                if (j9.getFEEInfo().size() == jvmOutput.getFEEInfo().size()){

                    return true;
                }
            }
        }
        return false;
    }

    public boolean ifJ9HotspotConsist(ArrayList<JvmOutput> j9xfuture, ArrayList<JvmOutput> others){

        for (JvmOutput other : others) {

            for (JvmOutput jvmOutput : j9xfuture) {

                for (String s : jvmOutput.getFEEInfo()) {

                    if (!other.getFEEInfo().contains(s)){
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
