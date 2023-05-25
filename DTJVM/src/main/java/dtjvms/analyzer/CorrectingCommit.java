package dtjvms.analyzer;
// dtjvms.analyzer.CorrectingCommit
import dtjvms.DTConfiguration;
import dtjvms.DTPlatform;
import dtjvms.JvmInfo;
import dtjvms.ProjectInfo;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.CFM.JvmOutput;
import dtjvms.executor.ExecutorHelper;
import dtjvms.loader.DTLoader;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CorrectingCommit {
    private static String rootPath = "/home/ningmo/Fasttailor/class_valid/";
    // jvm archive root path
    private static final String rootArchivePath = rootPath+"JVM_Archive";
    // project about
    private static final String projectPath = "sootOutput";
    private static String projectName = "eclipse-dacapo";      // 提供种子文件的项目
    private static final String provideName = "HotspotTests-Java"; //提供ingredient的项目
    private static final boolean projectPreDefineFlag = true;      // 此项目是否使用预定义类
    private static boolean crossProject = false;
    // execution result path
    private static String resultRootPath = rootPath+"testResult";
    private static String resultPath = "";
    private static final String dataListName = "dataList.csv";
    // classAssignment public key
    public static String UNKNOWN = "unknown";
    public static String UNIQUE = "unique";
    public static String DUPLICATE = "duplicate";
    // create sootOutPut file
    private static String shellCmd = "/bin/bash "+rootPath+"mkSootOutput.sh";
    private static String sootOutputPath = rootPath+"sootOutput";

    public static void main(String[] args) throws IOException, InterruptedException {

        if(args.length>0){
            resultPath = args[0];
        }
        Map<String,String> map = new HashMap<>();
        map.put("avrora","avrora-cvs-20091224");
        map.put("eclipse","eclipse-dacapo");
        map.put("fop","fop-dacapo");
        map.put("hotspot","HotspotTests-Java");
        map.put("openj9","Openj9Test-Test");
        map.put("pmd","pmd-4.2.5");
        map.put("sunflow","sunflow-0.07.2");
        map.put("jython","jython-dacapo");
        if(!resultPath.equals("")){
            Runtime run = Runtime.getRuntime();
            Process process = run.exec(shellCmd);
            InputStreamReader ips = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(ips);
            String line;
            StringBuilder out  = new StringBuilder();
            while ((line = br.readLine()) != null) {
                out.append(line);
            }
            for (String key : map.keySet()) {
                if(resultPath.contains(key)){
                    resultPath = resultRootPath+DTPlatform.FILE_SEPARATOR+
                            key+DTPlatform.FILE_SEPARATOR+
                            resultPath;
                    projectName = map.get(key);
                    break;
                }
            }
            System.out.println(resultPath);
            System.out.println(projectName);

            Map<String,List<String>> classAssignment = correctingCommit();
            splitOutputUniqueFile(classAssignment);
            System.exit(1);
        }

        File file = new File(resultRootPath);
        for (File listFile : Objects.requireNonNull(file.listFiles())) {
            projectName = map.get(listFile.getName());
            for (File file1 : Objects.requireNonNull(listFile.listFiles())) {
                if(file1.getName().contains("javatailor") || file1.getName().contains(".gz")){
                    continue;
                }
                resultPath = file1.getAbsolutePath();

                Runtime run = Runtime.getRuntime();
                Process process = run.exec(shellCmd);
                InputStreamReader ips = new InputStreamReader(process.getInputStream());
                BufferedReader br = new BufferedReader(ips);
                String line;
                StringBuilder out  = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    out.append(line);
                }

                System.out.println(resultPath);
                System.out.println(projectName);

                Map<String,List<String>> classAssignment = correctingCommit();
                splitOutputUniqueFile(classAssignment);

            }
        }
    }
    public static void splitOutputUniqueFile(Map<String,List<String>> classAssignment) throws IOException {
        File inputFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"outputUniqueFile.txt");
        File unknownFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"outputUniqueFile_unknown.txt");
        FileWriter unknownWriter = new FileWriter(unknownFile);
        File uniqueFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"outputUniqueFile_unique.txt");
        FileWriter uniqueWriter = new FileWriter(uniqueFile);
        File duplicateFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"outputUniqueFile_duplicate.txt");
        FileWriter duplicateWriter = new FileWriter(duplicateFile);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
        String line = bufferedReader.readLine();
        FileWriter fileWriter = null;
        while(line!=null){
            if (line.toLowerCase(Locale.ROOT).contains("difference found:")){
                String mutationName = line.substring(51);
                if(classAssignment.get(UNKNOWN).contains(mutationName)){
                    fileWriter = unknownWriter;
                }
                else if(classAssignment.get(UNIQUE).contains(mutationName)){
                    fileWriter = uniqueWriter;
                }
                else if(classAssignment.get(DUPLICATE).contains(mutationName)){
                    fileWriter = duplicateWriter;
                }else {
                    fileWriter = unknownWriter;
                }

            }
            fileWriter.write(line+"\n");
            fileWriter.flush();
            line=bufferedReader.readLine();
        }
        unknownWriter.flush();
        unknownWriter.close();
        uniqueWriter.flush();
        uniqueWriter.close();
        duplicateWriter.flush();
        duplicateWriter.close();
    }

    public static Map<String,List<String>> correctingCommit() throws IOException {
        // load jvm
        List<JvmInfo> jvmCmds = loadJvmCmd();

        // load project
        crossProject = !Objects.equals(projectName, provideName);
//        ProjectInfo targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath(resultPath+DTPlatform.FILE_SEPARATOR+projectPath, projectName, null, projectPreDefineFlag);
//        ProjectInfo provideProject = DTLoader.getInstance().loadTargetProjectWithGivenPath(resultPath+DTPlatform.FILE_SEPARATOR+projectPath, provideName, null, projectPreDefineFlag);
        ProjectInfo targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath(sootOutputPath, projectName, null, projectPreDefineFlag);
        ProjectInfo provideProject = DTLoader.getInstance().loadTargetProjectWithGivenPath(sootOutputPath, provideName, null, projectPreDefineFlag);

        System.out.println(targetProject);
        System.out.println(provideProject);

        // load output difference reveal class
        Map<String,String> class2path = loadDifferenceClass(targetProject.getSrcClassPath());
        // execute , save and analysis each class
        List<String> jvmIds = new ArrayList<>();
        for (JvmInfo jvmCmd : jvmCmds) {
            jvmIds.add(jvmCmd.getJvmId());
        }
        Collections.sort(jvmIds);

        Map<String,List<String>> classAssignment = new HashMap<>();
        classAssignment.put(UNKNOWN,new ArrayList<>());
        classAssignment.put(UNIQUE,new ArrayList<>());
        classAssignment.put(DUPLICATE,new ArrayList<>());

        List<List<Integer>> numStdoutUnique = new ArrayList<>();

        List<StringBuilder> dataList = new ArrayList<>();

        for (String pathKey : class2path.keySet()) {

            System.out.println("source class:"+pathKey);
            System.out.println("target class"+class2path.get(pathKey));
            // copy diffClass to project src
            File sourceFile = new File(pathKey);
            File targetFile = new File(class2path.get(pathKey));
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String className = sourceFile.getName();
            className = className.substring(0, className.lastIndexOf("_"));

            // execute
            HashMap<String, JvmOutput> results = executeClass(jvmCmds,targetProject,provideProject,className);

            // save
            Map<String,List<String>> stdoutMap = new HashMap<>(); // the std out of class by each jvm
            List<String> stdoutUnique = new ArrayList<>(); // the unique std out
            Map<String,List<Integer>> numStdoutMap = new HashMap<>(); // The std out represented by numerically
            saveExecuteResult(jvmIds,results,stdoutMap,stdoutUnique,numStdoutMap);
            for (String key : numStdoutMap.keySet()) {
                StringBuilder line = new StringBuilder();
                line.append(key).append(",");
                for (Integer integer : numStdoutMap.get(key)) {
                    line.append(integer.toString()).append(",");
                }
                dataList.add(line);
            }


            // analysis
            resultAnalysis(sourceFile.getName(), numStdoutMap, numStdoutUnique, classAssignment);
        }
        File outFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+dataListName);
        outFile.createNewFile();
        exportCsv(outFile,dataList);


        return classAssignment;
    }
    private static void resultAnalysis(String className, Map<String,List<Integer>> numStdoutMap, List<List<Integer>> numStdoutUnique, Map<String,List<String>> classAssignment) throws IOException {
        List<Integer> numList = new ArrayList<>();
        boolean unknownFlag = false;
        for (String key : numStdoutMap.keySet()) {
            List<Integer> value = numStdoutMap.get(key);
            numList.addAll(value);
            int stepTime = 0;
            int lastLevel = -1;
            for (Integer level : value) {
                if(lastLevel==-1){
                    lastLevel = level;
                    continue;
                }
                if(lastLevel!=level){
                    stepTime++;
                    lastLevel = level;
                }
            }
            if(stepTime > value.size()/2){ // class is unknown/falsePositive bug
                classAssignment.get(UNKNOWN).add(className);
                unknownFlag = true;
            }
        }
        if(!unknownFlag){
            if(!numStdoutUnique.contains(numList)){
                numStdoutUnique.add(numList);
                classAssignment.get(UNIQUE).add(className);
                File out = new File("filterResult.txt");
                FileWriter fileWriter = new FileWriter(out,true);
                String tempS = resultPath.substring(0,resultPath.length());
                fileWriter.write(tempS.substring(tempS.lastIndexOf("/")+1)+":");
                fileWriter.write(className+"\n");
                fileWriter.flush();
                fileWriter.close();
            }else {
                classAssignment.get(DUPLICATE).add(className);
            }
        }
    }
    private static void saveExecuteResult(List<String> jvmIds,HashMap<String, JvmOutput> results,Map<String,List<String>> stdoutMap,List<String> stdoutUnique,Map<String,List<Integer>> numStdoutMap){
        for (String resultKey : jvmIds) { // the results can be out of order (depending on the execution time), the fixed jvmIds are used here

            String developer = resultKey.split("-")[0];
            String version = resultKey.split("-")[1];
            String newKey = developer+"-"+version;

            if (!stdoutMap.containsKey(newKey)){
                stdoutMap.put(newKey,new ArrayList<>());
                numStdoutMap.put(newKey,new ArrayList<>());
            }

            String stdout = results.get(resultKey).getStdout();
            if(!stdoutUnique.contains(stdout)){
                stdoutUnique.add(stdout);
            }

            stdoutMap.get(newKey).add(stdout);
            numStdoutMap.get(newKey).add(stdoutUnique.indexOf(stdout));
        }
    }


    private static HashMap<String, JvmOutput> executeClass(List<JvmInfo> jvmCmds,ProjectInfo targetProject,ProjectInfo provideProject,String className){
        HashMap<String, JvmOutput> results = new HashMap<>();
        for (JvmInfo cmd : jvmCmds) {
            System.out.println("= = = = = = = = = = = = = " + cmd.getJvmId() + " = = = = = = = = = = = = = = = = = = ");
            String javaCmd = cmd.getJavaCmd();

            ArrayList<String> projOptions = targetProject.getProjoptions();
            String[] projOptionsArray = projOptions.toArray(new String[projOptions.size()]);
            String cmdExecutor;
            if (crossProject){
                cmdExecutor = ExecutorHelper.assembleJavaCmd(javaCmd,
                        null,
                        targetProject.getpClassPath() + DTPlatform.PATH_SEPARATOR + provideProject.getpClassPath(),
                        className,
                        false,
                        projOptionsArray);
            } else {
                cmdExecutor = ExecutorHelper.assembleJavaCmd(javaCmd,
                        null,
                        targetProject.getpClassPath(),
                        className,
                        false,
                        projOptionsArray);
            }
            System.out.println(cmdExecutor);
            String jvmId = cmd.getJvmId();
            Thread test = new Thread(new Runnable() {
                @Override
                public void run() {

                    CFMExecutor.getInstance().execute(cmdExecutor);
                }
            });
            test.start();
            try {
                TimeUnit.SECONDS.timedJoin(test, DTConfiguration.CLASS_MAX_RUNTIME);
                CFMExecutor.getInstance().shutDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            JvmOutput currentOutput = CFMExecutor.getInstance().getCurrentOutput();
            if (CFMExecutor.getInstance().getCurrentOutput() != null){
                if(currentOutput.getStdout().contains("my_check_sum_value:")){
                    currentOutput.setStdout(currentOutput.getStdout().substring(currentOutput.getStdout().indexOf("my_check_sum_value:"))+"\n");
                    currentOutput.setStdout(currentOutput.getStdout().substring(0,currentOutput.getStdout().indexOf("\n")).replace("my_check_sum_value:",""));
                }else {
                    currentOutput.setStdout("");
                }
                System.out.println("stdout:"+currentOutput.getStdout());
                System.out.println("stderr:"+currentOutput.getStderr());
                results.put(jvmId, currentOutput);
            } else {
                System.out.println("TIMEOUT");
                results.put(jvmId, new JvmOutput(""));
            }
        }
        return results;
    }
    private static List<JvmInfo> loadJvmCmd(){

        List<JvmInfo> jvms = new ArrayList<>();
        File rootArchive = new File(rootArchivePath);
        File[] developers = rootArchive.listFiles();
        assert developers != null;
        for (File developer : developers) {
            File[] versions = developer.listFiles();
            assert versions != null;
            for (File version : versions) {
                File[] exes = version.listFiles();
                assert exes != null;
                for (File exe : exes) {
                    jvms.add(new JvmInfo(developer.getAbsolutePath(),"",developer.getName(),version.getName()+"-"+exe.getName(),exe.getAbsolutePath()+"/bin/java"));
                }
            }
        }
        return jvms;
    }

    private static Map<String,String> loadDifferenceClass(String targetProjectSrcPath) throws IOException {

        // 获得 diffClass 与 目标路径 之间的 map
        File outUniqueFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"outputUniqueFile.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(outUniqueFile));
        String line = bufferedReader.readLine();
        Map<String,String> class2newPath = new HashMap<>();
        while (line!=null){
            if (line.toLowerCase(Locale.ROOT).contains("difference found:")){
                String mutationName = line.substring(line.lastIndexOf("-")+1,line.length()-6);
                String className = mutationName.substring(0,mutationName.length()-19);
                StringBuilder classPath = new StringBuilder(targetProjectSrcPath);
                for (String s : className.split("\\.")) {
                    classPath.append(DTPlatform.FILE_SEPARATOR).append(s);
                }
                class2newPath.put(mutationName+".class",classPath.append(".class").toString());
            }
            line = bufferedReader.readLine();
        }
        // 获得 diffClass 的 history文件路径
        Set<String> mutationsName = class2newPath.keySet();
        Map<String,String> oldPath2newPath = new HashMap<>();
        File historyFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"04SynthesisHistory");
        if (!historyFile.exists()) {
            historyFile = new File(resultPath+DTPlatform.FILE_SEPARATOR+"03SynthesisHistory");
        }
        for (File timeStampFile : Objects.requireNonNull(historyFile.listFiles())) {
            for (File diffClassDir : Objects.requireNonNull(
                    new File(
                            timeStampFile.getAbsoluteFile() + DTPlatform.FILE_SEPARATOR + "diffClass"
                    ).listFiles())) {
                for (File diffClass : Objects.requireNonNull(diffClassDir.listFiles())) {
                    if (diffClass.getName().endsWith("class") && mutationsName.contains(diffClass.getName())){
                        oldPath2newPath.put(diffClass.getAbsolutePath(), class2newPath.get(diffClass.getName()));
                    }
                }
            }
        }
        return oldPath2newPath;
    }

    private static boolean exportCsv(File file, List<StringBuilder> dataList){
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
