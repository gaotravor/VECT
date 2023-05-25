package dtjvms.analyzer;

import dtjvms.executor.CFM.JvmOutput;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @Auther: Gao Tianchang
 * @Date: 2021/12/14/8:24
 * @Description:
 */
public class UniqueFilter{
    // 存储键值对列表，键值对的信息为 虚拟机信息：FEE列表
    private static List<Map<String,List<String>>> uniqueFEE = new ArrayList<>();
    // 存储键值对列表，键值对的信息为 虚拟机信息：简化的错误信息
    private static List<Map<String,String>> uniqueDiffInfo = new ArrayList<>();
    private static List<String> uniqueException = new ArrayList<>();
    private static List<String> unKnowUniqueException = new ArrayList<>();
    private static List<String> uniqueStdout = new ArrayList<>();
    private static List<String> unKnowUniqueStdout = new ArrayList<>();
    private static final String logFileRootPath = "I:\\历史文件备份\\JVM_Testing\\VECT\\test record\\rebuttal\\Javatailor_checksum";
    private static final String timeoutFilePath = logFileRootPath + "\\timeoutFile.txt"; // 存放time有关的差异"
    private static final String exceptionRepeatFilePath = logFileRootPath + "\\exceptionRepeatFile.txt"; // 存放重复的异常差异
    private static final String exceptionUniqueFilePath = logFileRootPath + "\\exceptionUniqueFile.txt"; // 存放unique的异常差异
    private static final String unKnowRepeatPath = logFileRootPath + "\\unKnowRepeatFile.txt"; // 存放未修复的重复差异
    private static final String unKnowUniquePath = logFileRootPath + "\\unKnowUniqueFile.txt"; // 存放未修复的unique差异
    private static final String outputRepeatPath = logFileRootPath + "\\outputRepeatFile.txt"; // 存放output的重复差异
    private static final String outputUniquePath = logFileRootPath + "\\outputUniqueFile.txt"; // 存放output的重复差异

    @Test
    public void test() throws IOException {
        File root = new File(logFileRootPath);
        List<Long> timeStamps = new ArrayList<>();
        for (File file : Objects.requireNonNull(root.listFiles())) {
            if(file.getName().toLowerCase(Locale.ROOT).contains("unique")){
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line = bufferedReader.readLine();
                while (line!=null){
                    if(line.contains("Difference found:")){
                        timeStamps.add(Long.parseLong(line.substring(line.length()-19,line.length()-6)));
                    }
                    line = bufferedReader.readLine();
                }
            }

        }
        Map<Long,Integer> map = new TreeMap(new HashMap<>());
        Collections.sort(timeStamps);
        Long startTime = timeStamps.get(0);
        long max = -1;
        for(int i=0;i<timeStamps.size();i++){
            timeStamps.set(i,(timeStamps.get(i)-startTime)/1000/60);
            Long index = timeStamps.get(i);
            if(index > max){
                max = index;
            }
            if(!map.containsKey(index)){
                map.put(index,0);
            }
            map.replace(index,map.get(index)+1);
        }
        for(long i = 0;i<max;i++){
            if(!map.containsKey(i)){
                map.put(i,0);
            }
        }
        int sum = 0;
        List<StringBuilder> sumList = new ArrayList<>();
        for (Long aLong : map.keySet()) {
            sum = sum + map.get(aLong);
            sumList.add(new StringBuilder(Integer.toString(sum)));
        }
        File csvFile = new File(logFileRootPath+"\\count.csv");
        exportCsv(csvFile,sumList);
    }
    public static void main(String[] args) throws IOException {
        run();
    }

    public static void run()throws IOException {
        soutSelectTime();
        createAllDiffFile();
        File tempFile = new File(timeoutFilePath);
        tempFile.delete();
        tempFile.createNewFile();
        tempFile = new File(exceptionRepeatFilePath);
        tempFile.delete();
        tempFile.createNewFile();
        tempFile = new File(exceptionUniqueFilePath);
        tempFile.delete();
        tempFile.createNewFile();
        tempFile = new File(unKnowRepeatPath);
        tempFile.delete();
        tempFile.createNewFile();
        tempFile = new File(unKnowUniquePath);
        tempFile.delete();
        tempFile.createNewFile();
        tempFile = new File(outputRepeatPath);
        tempFile.delete();
        tempFile.createNewFile();
        tempFile = new File(outputUniquePath);
        tempFile.delete();
        tempFile.createNewFile();
        File file = new File(logFileRootPath+"\\difference.log");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        String line = null;
        List<String> diffInfos = new ArrayList<>();
        List<Integer> keyIndex = new ArrayList<>();
        boolean firstFlag = true;
        int timeoutTime = 0;
        int exceptionUniqueTime = 0;
        int exceptionRepeatTime = 0;
        int outputUniqueTime = 0;
        int outputRepeatTime = 0;
        int unKnowUniqueTime = 0;
        int unKnowRepeatTime = 0 ;
        while ((line = br.readLine())!=null){
            if(line.contains("Difference found")){
                if(firstFlag){
                    firstFlag = false;
                }else {
                    keyIndex.add(diffInfos.size());
                    switch(filterDiffInfo(diffInfos,keyIndex)){
                        case 0:timeoutTime++;break;
                        case 1:exceptionUniqueTime++;break;
                        case 2:exceptionRepeatTime++;break;
                        case 3:outputUniqueTime++;break;
                        case 4:outputRepeatTime++;break;
                        case 5:unKnowUniqueTime++;break;
                        case 6:unKnowRepeatTime++;break;
                    }
                    diffInfos.clear();
                    keyIndex.clear();
                }
            }
            else if(line.contains("================")){
                keyIndex.add(diffInfos.size());
            }
            diffInfos.add(line);
        }
        keyIndex.add(diffInfos.size());
        switch(filterDiffInfo(diffInfos,keyIndex)){
            case 0:timeoutTime++;break;
            case 1:exceptionUniqueTime++;break;
            case 2:exceptionRepeatTime++;break;
            case 3:outputUniqueTime++;break;
            case 4:outputRepeatTime++;break;
            case 5:unKnowUniqueTime++;break;
            case 6:unKnowRepeatTime++;break;
        }
        System.out.println("timeoutTime:"+timeoutTime);
        System.out.println("exceptionTime:"+(exceptionRepeatTime+exceptionUniqueTime));
        System.out.println("exceptionUniqueTime:"+exceptionUniqueTime);
        System.out.println("outputTime:"+(outputRepeatTime+outputUniqueTime));
        System.out.println("outputUniqueTime:"+outputUniqueTime);
        System.out.println("unKnowTime:"+(unKnowRepeatTime+unKnowUniqueTime));
        System.out.println("unKnowUniqueTime:"+unKnowUniqueTime);

    }

    private static void soutSelectTime() throws IOException {
        int sum = 0;
        File results = new File(logFileRootPath+"\\03results");
        for (File timeStamp : Objects.requireNonNull(results.listFiles())) {
            if (!timeStamp.isDirectory()){
                continue;
            }
            int lineCount = 0;
            File timeFile = new File(Objects.requireNonNull(timeStamp.listFiles())[0].getAbsolutePath()+"\\DiffAndSelectTime.txt");
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(timeFile));
            } catch (FileNotFoundException e) {
                System.out.println(Objects.requireNonNull(timeStamp.listFiles())[0].getAbsolutePath()+":执行失败");
                continue;
            }
            String line = bufferedReader.readLine();
            String lastLine = "";
            while (line!=null){
                lineCount++;
                lastLine = line;
                line = bufferedReader.readLine();
            }
            sum = sum + Integer.parseInt(lastLine.split(",")[1]);
            System.out.println(Objects.requireNonNull(timeStamp.listFiles())[0].getAbsolutePath()+":"+lineCount);

        }
        System.out.println("select:"+sum);
    }
    private static void createAllDiffFile() throws IOException {
        File results = new File(logFileRootPath+"\\03results");
        File allDiff = new File(logFileRootPath+"\\difference.log");
        allDiff.delete();
        allDiff.createNewFile();
        FileWriter fileWriter = new FileWriter(allDiff,true);
        for (File timeStamp : Objects.requireNonNull(results.listFiles())) {
            if (!timeStamp.isDirectory()){
                continue;
            }
            File timeFile = new File(Objects.requireNonNull(timeStamp.listFiles())[0].getAbsolutePath()+"\\difference.log");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(timeFile));
            String line = bufferedReader.readLine();
            while (line!=null){
                fileWriter.write(line+"\n");
                line = bufferedReader.readLine();
            }
        }
        fileWriter.flush();
        fileWriter.close();
    }
    public static int filterDiffInfo(List<String> diffInfos, List<Integer> keyIndex) throws IOException {

//        HashMap<String, JvmOutput> results = new HashMap<>();
        String allException = diffInfos.get(1);
        Map<String,String> allStdout = new HashMap<>();
        boolean unKnowFlag = false;
        for(int i=0;i<keyIndex.size()-1;i++){
            Integer starIndex = keyIndex.get(i);
            Integer endIndex = keyIndex.get(i+1);
//            String key = diffInfos.get(starIndex).replace("=","");
            StringBuilder stderr = new StringBuilder();
            // 如果里面有timeout
            for(int j=starIndex+2;j<endIndex;j++){
                if(     diffInfos.get(j).toLowerCase().contains("timeout") ||
                        diffInfos.get(j).toLowerCase().contains("not enough space") ||
                        diffInfos.get(j).toLowerCase().contains("cannot allocate memory")){
                    File timeoutFile = new File(timeoutFilePath);
                    FileWriter fileWriter = new FileWriter(timeoutFile,true);
                    for(String s:diffInfos){
                        fileWriter.write(s+"\n");
                    }
                    fileWriter.flush();
                    fileWriter.close();
                    return 0;
                }
                stderr.append("\n");
                stderr.append(diffInfos.get(j));
            }
            // 如果差异是一直存在的
            if(diffInfos.get(starIndex+1).toLowerCase(Locale.ROOT).contains("for rules inconsistent")){
                unKnowFlag =true;
            }
//            JvmOutput jvmOutput;
            String stdout = stderr.toString();
            if(stdout.contains("my_check_sum_value:")){
                stdout = stdout.substring(stdout.indexOf("my_check_sum_value:"))+"\n";
                stdout = stdout.substring(0,stdout.indexOf("\n"));
                stdout = stdout.replace("my_check_sum_value:","");
            }else {
                stdout = "";
            }

            String exeName = diffInfos.get(starIndex);
            exeName = exeName.replace("=","");
            if(exeName.toLowerCase(Locale.ROOT).contains("hotspot")){
                if(exeName.contains("-XX")){
                    exeName = exeName.substring(0,exeName.indexOf("-XX"));
                }
                if(exeName.toLowerCase(Locale.ROOT).contains("old")){
                    allStdout.put("hotspot-old",stdout);
                }else {
                    allStdout.put("hotspot-new",stdout);
                }
            }else if (exeName.toLowerCase(Locale.ROOT).contains("openj9")){
                if(diffInfos.get(starIndex).toLowerCase(Locale.ROOT).contains("old")){
                    allStdout.put("openj9-old",stdout);
                }else {
                    allStdout.put("openj9-new",stdout);
                }
            }else if (exeName.toLowerCase(Locale.ROOT).contains("bisheng")){
                if(diffInfos.get(starIndex).toLowerCase(Locale.ROOT).contains("old")){
                    allStdout.put("bisheng-old",stdout);
                }else {
                    allStdout.put("bisheng-new",stdout);
                }
            }
//            jvmOutput = new JvmOutput(stdout,stderr.toString());
//
//
//            results.put(key,jvmOutput);
        }
        String stdout = "";
        stdout = stdout + allStdout.get("hotspot-old");
        stdout = stdout + allStdout.get("hotspot-new");
        stdout = stdout + allStdout.get("openj9-old");
        stdout = stdout + allStdout.get("openj9-new");
        stdout = stdout + allStdout.get("bisheng-old");
        stdout = stdout + allStdout.get("bisheng-new");
        boolean isException = allException.toLowerCase(Locale.ROOT).contains("exception") ||
                allException.toLowerCase(Locale.ROOT).contains("error") ||
                allException.toLowerCase(Locale.ROOT).contains("failure");
        if(unKnowFlag){
            if(isException){
                if(!unKnowUniqueException.contains(allException)){
                    unKnowUniqueException.add(allException);
                    File uniqueFile = new File(unKnowUniquePath);
                    FileWriter fileWriter = new FileWriter(uniqueFile,true);
                    for(String s:diffInfos){
                        fileWriter.write(s+"\n");
                    }
                    fileWriter.flush();
                    fileWriter.close();
                    return 5;
                }else {
                    File repeatFile = new File(unKnowRepeatPath);
                    FileWriter fileWriter = new FileWriter(repeatFile,true);
                    for(String s:diffInfos){
                        fileWriter.write(s+"\n");
                    }
                    fileWriter.flush();
                    fileWriter.close();
                    return 6;
                }
            }else{
                if(!unKnowUniqueStdout.contains(stdout)){
                    unKnowUniqueStdout.add(stdout);
                    File uniqueFile = new File(unKnowUniquePath);
                    FileWriter fileWriter = new FileWriter(uniqueFile,true);
                    for(String s:diffInfos){
                        fileWriter.write(s+"\n");
                    }
                    fileWriter.flush();
                    fileWriter.close();
                    return 5;
                }else {
                    File uniqueFile = new File(unKnowRepeatPath);
                    FileWriter fileWriter = new FileWriter(uniqueFile,true);
                    for(String s:diffInfos){
                        fileWriter.write(s+"\n");
                    }
                    fileWriter.flush();
                    fileWriter.close();
                    return 6;
                }
            }
        }

        // 如果是输出型差异
        if(isException){
            if(!uniqueException.contains(allException)){
                uniqueException.add(allException);
                File uniqueFile = new File(exceptionUniqueFilePath);
                FileWriter fileWriter = new FileWriter(uniqueFile,true);
                for(String s:diffInfos){
                    fileWriter.write(s+"\n");
                }
                fileWriter.flush();
                fileWriter.close();
                return 1;
            }else {
                File repeatFile = new File(exceptionRepeatFilePath);
                FileWriter fileWriter = new FileWriter(repeatFile,true);
                for(String s:diffInfos){
                    fileWriter.write(s+"\n");
                }
                fileWriter.flush();
                fileWriter.close();
                return 2;
            }
        } else{
            if(!uniqueStdout.contains(stdout)){
                uniqueStdout.add(stdout);
                File uniqueFile = new File(outputUniquePath);
                FileWriter fileWriter = new FileWriter(uniqueFile,true);
                for(String s:diffInfos){
                    fileWriter.write(s+"\n");
                }
                fileWriter.flush();
                fileWriter.close();
                return 3;
            }else {
                File uniqueFile = new File(outputRepeatPath);
                FileWriter fileWriter = new FileWriter(uniqueFile,true);
                for(String s:diffInfos){
                    fileWriter.write(s+"\n");
                }
                fileWriter.flush();
                fileWriter.close();
                return 4;
            }
        }


//        FilterChain filterChain = new FilterChain();
//        filterChain.addFilter(new StdErrFilter());
//        filterChain.startFilter(results);



//        for(String key : results.keySet()){
//            JvmOutput jvmOut = results.get(key);
//            List<String> temp =jvmOut.getFEEInfo();
//            List<String> exceptions = new ArrayList<>();
//            exceptions.add("FileNotFoundException");
//            for (String exception : exceptions) {
//                if(temp.contains(exception)){
//                    File repeatFile = new File(repeatFilePath);
//                    FileWriter fileWriter = new FileWriter(repeatFile,true);
//                    for(String s:diffInfos){
//                        fileWriter.write(s+"\n");
//                    }
//                    fileWriter.flush();
//                    fileWriter.close();
//                    return 1;
//                }
//            }
//            List<String> strings = new ArrayList<>();
//
//            for (String string : strings) {
//                if(diffInfos.get(0).contains(string)){
//                    File repeatFile = new File(repeatFilePath);
//                    FileWriter fileWriter = new FileWriter(repeatFile,true);
//                    for(String s:diffInfos){
//                        fileWriter.write(s+"\n");
//                    }
//                    fileWriter.flush();
//                    fileWriter.close();
//                    return 1;
//                }
//            }
//
//        }
//        File uniqueFile = new File(uniqueFilePath);
//        FileWriter fileWriter = new FileWriter(uniqueFile,true);
//        for(String s:diffInfos){
//            fileWriter.write(s+"\n");
//        }
//        fileWriter.flush();
//        fileWriter.close();
//
//        return 2;



    }
    public static int analysisDiffInfo(List<String> diffInfos,List<Integer> keyIndex) throws IOException {
        HashMap<String, JvmOutput> results = new HashMap<>();
        for(int i=0;i<keyIndex.size()-1;i++){
            Integer starIndex = keyIndex.get(i);
            Integer endIndex = keyIndex.get(i+1);
            String key = diffInfos.get(starIndex).replace("=","");
            StringBuilder stderr = new StringBuilder();
            for(int j=starIndex+2;j<endIndex;j++){
                if(diffInfos.get(j).toLowerCase().contains("timeout")){
                    File timeoutFile = new File(timeoutFilePath);
                    FileWriter fileWriter = new FileWriter(timeoutFile,true);
                    for(String s:diffInfos){
                        fileWriter.write(s+"\n");
                    }
                    fileWriter.flush();
                    fileWriter.close();
                    return 0;
                }
                stderr.append("\n");
                stderr.append(diffInfos.get(j));
            }
            JvmOutput jvmOutput = new JvmOutput("", stderr.toString());
            results.put(key,jvmOutput);
        }
        FilterChain filterChain = new FilterChain();
        filterChain.addFilter(new StdErrFilter());
        filterChain.startFilter(results);

        for (String s : results.keySet()) {
            JvmOutput jvmOutput = results.get(s);
            if(jvmOutput.getFEEInfo().toString().contains("[]")){
                return 0;
            }
        }

        if(doFilter(results)){
            File repeatFile = new File(exceptionRepeatFilePath);
            FileWriter fileWriter = new FileWriter(repeatFile,true);
            for(String s:diffInfos){
                fileWriter.write(s+"\n");
            }
            fileWriter.flush();
            fileWriter.close();
            return 1;
        }else{
            File uniqueFile = new File(exceptionUniqueFilePath);
            FileWriter fileWriter = new FileWriter(uniqueFile,true);
            for(String s:diffInfos){
                fileWriter.write(s+"\n");
            }
            fileWriter.flush();
            fileWriter.close();

            return 2;
        }
    }
    /**
     * 过滤重复生成的差异类
     * @param results
     * @return T代表是重复的，过滤生效了 F代表是unique的，过滤无效
     */
    public static boolean doFilter(HashMap<String, JvmOutput> results) {
        Map<String,List<String>> newFEE = new HashMap<>();
        Map<String,String> newDiffInfo = new HashMap<>();
        for(String key : results.keySet()){
            JvmOutput jvmOut = results.get(key);
            List<String> temp =jvmOut.getFEEInfo();
            newFEE.put(key,temp);
            newDiffInfo.put(key,simplifyStdErr(jvmOut.getStderr()));
        }
        boolean firstCheckFlag = false;
        int equalFEEIndex = 0;
        for(Map<String,List<String>> FEE:uniqueFEE){
            if (equalCheck(FEE,newFEE)){
                firstCheckFlag = true;
                break;
            }
            equalFEEIndex++;
        }
        if(firstCheckFlag){
            double sum = 0;
            Map<String,String> DiffInfo = uniqueDiffInfo.get(equalFEEIndex);
            for(String key:DiffInfo.keySet()){
                sum += CosSimilarty.run(DiffInfo.get(key),newDiffInfo.get(key));
            }
            if(sum/DiffInfo.keySet().size() > 0.6){
                return true;
            }else {
                uniqueFEE.add(newFEE);
                uniqueDiffInfo.add(newDiffInfo);
                return false;
            }
        }else {
            uniqueFEE.add(newFEE);
            uniqueDiffInfo.add(newDiffInfo);
            return false;
        }
    }

    /**
     * 这里写的不好，跑出来结果之后要进行改进
     * @param stdErr
     * @return
     */
    private static String simplifyStdErr(String stdErr){
        String[] lines = stdErr.split("\n");
        if(stdErr.contains("Error")){
            int reasonIndex = 0;
            for(String line:lines){
                if(line.contains("Reason")){
                    reasonIndex++;
                    break;
                }
                reasonIndex++;
            }
            if(lines.length == reasonIndex){
                return  stdErr;
            }
            return lines[0] + " "+lines[reasonIndex];
        }else{
            StringBuilder result = new StringBuilder();
            for(String line:lines){
                if(line.contains("java.")){
                    result.append(line).append(" ");
                }
            }
            return result.toString();
        }
    }
    private static boolean equalCheck(Map<String,List<String>> map1, Map<String,List<String>> map2){
        boolean isSame = true;
        for(String s:map1.keySet()){
            if(!map1.get(s).equals(map2.get(s))){
                isSame = false;
                break;
            }
        }
        return isSame;
    }
    public static void logRepeatJvmOutput(String projName, String className, DiffCore diff, HashMap<String, JvmOutput> results) throws IOException {
        File file = new File("RepeatJvmOutput.txt");
        FileWriter fileWriter = new FileWriter(file,true);

        fileWriter.write("Difference found: project-" + projName + "@class-" + className+"\n");
        for (String s : results.keySet()) {
            fileWriter.write(String.join("", Collections.nCopies(50,"=")) + s + String.join("", Collections.nCopies(50,"="))+"\n");
            fileWriter.write(String.valueOf(diff.getDiffMessage() + ":" + results.get(s).getFEEInfo())+"\n");
            fileWriter.write(results.get(s)+"\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }

    public static boolean exportCsv(File file, List<StringBuilder> dataList){
        boolean isSucess=false;

        FileOutputStream out=null;
        OutputStreamWriter osw=null;
        BufferedWriter bw=null;
        try {
            out = new FileOutputStream(file);
            osw = new OutputStreamWriter(out);
            bw =new BufferedWriter(osw);
            if(dataList!=null && !dataList.isEmpty()){
                for(StringBuilder data : dataList){
                    bw.append(data).append("\r");
                }
            }
            isSucess=true;
        } catch (Exception e) {
            isSucess=false;
        }finally{
            if(bw!=null){
                try {
                    bw.close();
                    bw=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(osw!=null){
                try {
                    osw.close();
                    osw=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(out!=null){
                try {
                    out.close();
                    out=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return isSucess;
    }
}
