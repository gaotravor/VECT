package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StdErrFilter extends Filter{

    //    private static final String jvmwarningmsg = ".* VM warning:.*";
    private static final String COMMON_EXCEPTION_KEYWORD_PATTERN = "[A-Za-z]+?Exception";
    private static final String COMMON_ERROR_KEYWORD_PATTERN = "[A-Za-z]+?Error";
    private static final String COMMON_FAILURE_KEYWORD_PATTERN = "[A-Za-z]+?Failure";


    @Override
    protected void doFilter(HashMap<String, JvmOutput> results) {

        Pattern errorPattern = Pattern.compile(COMMON_ERROR_KEYWORD_PATTERN, Pattern.MULTILINE);
        Pattern exceptionPattern = Pattern.compile(COMMON_EXCEPTION_KEYWORD_PATTERN, Pattern.MULTILINE);
        Pattern failurePattern = Pattern.compile(COMMON_FAILURE_KEYWORD_PATTERN, Pattern.MULTILINE);

        for (String key : results.keySet()) {

            JvmOutput jvmOut = results.get(key);

            if (!jvmOut.getOutput().isEmpty()){

                List<String> outputs = jvmOut.asLines();
                for (String output : outputs) {
                    if(output.contains("Segmentation error")||output.contains("core dump") || output.contains("javacore.")){
                        jvmOut.getErrors().add("Segmentation error");
                        break;
                    }
                    if(output.contains("JvmOutput-TIMEOUT")){
                        jvmOut.getErrors().add("JvmOutput-TIMEOUT");
                        break;
                    }
//                    if(output.contains("Not enough space")){
//                        jvmOut.getErrors().add("Not enough space");
//                        break;
//                    }
                    output = output + "\n";
                    Matcher errorMatcher = errorPattern.matcher(output);
                    while (errorMatcher.find()){
                        if (!jvmOut.getErrors().contains(errorMatcher.group(0))){
                            jvmOut.getErrors().add(errorMatcher.group(0));
                        }
                    }
                    Matcher exceptionMatcher = exceptionPattern.matcher(output);
                    while (exceptionMatcher.find()){
                        if (!jvmOut.getExceptions().contains(exceptionMatcher.group(0))){
                            jvmOut.getExceptions().add(exceptionMatcher.group(0));
                        }
                    }
                    Matcher failureMatcher = failurePattern.matcher(output);
                    while (failureMatcher.find()){
                        if (!jvmOut.getFailures().contains(failureMatcher.group(0))){
                            jvmOut.getFailures().add(failureMatcher.group(0));
                        }
                    }
                }
            }
        }
    }
}
