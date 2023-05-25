package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JunitFilter extends Filter{

//    private static final String JUNIT_ERR_LINE_PATTERN = "\\d+\\) .*\\(.*\\)";

    private static final List<String> FILTER_STRINGS = new ArrayList<>();

    static {
        FILTER_STRINGS.add("JUnit version");
        FILTER_STRINGS.add("Time:");
        FILTER_STRINGS.add("OK (");
    }

    @Override
    protected void doFilter(HashMap<String, JvmOutput> results) {

//        String filterOut = "";
        for (String key : results.keySet()) {

            JvmOutput jvmOut = results.get(key);
            StringBuilder filterOut = new StringBuilder();
            if (!jvmOut.getStdout().isEmpty()) {

                List<String> outputs = jvmOut.stdoutAsLines();

                boolean junitOutput = false;
                for (String output : outputs) {

                    if (output.startsWith("JUnit version")){
                        junitOutput = true;
                    }
                    if (output.startsWith("Time:")){
                        junitOutput = false;
                    }

                    if (junitOutput){
                        output = output.replace(".", "");
                    }
                    if (output.length() != 0){

                        boolean flag = false;
                        for (String filterString : FILTER_STRINGS) {

                            if (output.startsWith(filterString)){

                                flag = true;
                                break;
                            }
                        }
                        if (!flag){
                            filterOut.append(output).append("\n");
                        }
                    }
                }
                jvmOut.setStdout(filterOut.toString());
            }
        }
    }
}
