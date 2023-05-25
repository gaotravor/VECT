package core.utils;

import JimpleMixer.core.Configuration;
import core.ClassInfo;
import core.MainConfiguration;
import core.helper.MainHelper;
import dtjvms.*;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.CFM.JvmOutput;
import dtjvms.executor.ExecutorHelper;
import dtjvms.loader.DTLoader;
import soot.G;
import soot.Scene;

import java.io.*;
import java.util.*;

public class CorrectingCommit {
    /**
     * Here you can set up the project you want to test
     */
    // Provide seed files for the project
    public static String projectName = "HotspotTests-Java";
    public static boolean projectPreDefineFlag = true;
    // Provide ingredients for the project
    public static String mutationProviderProject = "HotspotTests-Java";
    public static boolean providerProjectPreDefineFlag = true;
    // pre-define file name
    public static String defineClassesPath = "testcases.txt";

    public static String timeStamp;
    public static List<ClassInfo> seeds;
    public static boolean crossProject = false;

    public static ArrayList<JvmInfo> allJvmCmd = new ArrayList<>();
    public static ArrayList<JvmInfo> importantJvmCmd = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        if (args.length < 5){
            System.out.println("Please set the para");
            System.out.println("1. rootPath");
            System.out.println("2. RQ?");
            System.out.println("3. benchmark");
            System.out.println("4. jvmPath");
            System.out.println("5. resultPath");
        }

        String root = args[0];
        String RQ = args[1];
        String Benchmark = args[2];
        File benchmarkDir = new File(root+DTPlatform.FILE_SEPARATOR+RQ+ DTPlatform.FILE_SEPARATOR +Benchmark);
        Map<String,String> projectMap = new HashMap<>();
        projectMap.put("avrora","avrora-cvs-20091224");
        projectMap.put("eclipse","eclipse-dacapo");
        projectMap.put("fop","fop-dacapo");
        projectMap.put("hotspot","HotspotTests-Java");
        projectMap.put("jython","jython-dacapo");
        projectMap.put("openj9","Openj9Test-Test");
        projectMap.put("sunflow","sunflow-0.07.2");
        projectMap.put("pmd","pmd-4.2.5");

        getAllJvmInfo(args[3]);
        for (File versionDir : Objects.requireNonNull(benchmarkDir.listFiles())) {
            if (!versionDir.isDirectory()) {
                continue;
            }
            for (File resultDir : Objects.requireNonNull(versionDir.listFiles())) {
                if (!resultDir.isDirectory()) {
                    continue;
                }
                if(args.length == 6){
                    if(!resultDir.getAbsolutePath().equals(args[5])){
                        continue;
                    }
                }
                String outFilePath = args[4];
                System.out.println(resultDir.getAbsolutePath());
                String rootPath = resultDir.getAbsolutePath();
                List<String> classFileList = getAllClassFile(new File(rootPath+DTPlatform.FILE_SEPARATOR+"04SynthesisHistory"));
                if(classFileList.size()==0){
                    continue;
                }

                List<String> targetClassNames = new ArrayList<>();
                File CorrectingCommit = new File(resultDir+DTPlatform.FILE_SEPARATOR+"CorrectingCommit.txt");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(CorrectingCommit));
                String line = bufferedReader.readLine();
                while(line!=null){
                    targetClassNames.add(line);
                    line=bufferedReader.readLine();
                }

                List<ClassInfo> diffClasses = new ArrayList<>();
                if(Objects.equals(DTPlatform.FILE_SEPARATOR, "\\")){
                    DTPlatform.FILE_SEPARATOR = "\\\\";
                }
                for (String classPath : classFileList) {
                    String originClassName = classPath.split(DTPlatform.FILE_SEPARATOR)[classPath.split(DTPlatform.FILE_SEPARATOR).length - 2];
                    String originClassPath = originClassName.replace(".",DTPlatform.FILE_SEPARATOR)+".class";
                    String className = classPath.split(DTPlatform.FILE_SEPARATOR)[classPath.split(DTPlatform.FILE_SEPARATOR).length - 1];
                    String pathToClass = classPath;
                    if(!targetClassNames.contains(className)){
                        continue;
                    }
                    ClassInfo classInfo = new ClassInfo(originClassName,originClassPath,className,pathToClass,false,0,0,false);
                    diffClasses.add(classInfo);
                }
                projectName = rootPath.split(RQ)[1].split(DTPlatform.FILE_SEPARATOR)[1];
                for (String key : projectMap.keySet()) {
                    if(projectName.contains(key)){
                        projectName = key;
                        break;
                    }
                }
                projectName = projectMap.get(projectName);
                mutationProviderProject = "HotspotTests-Java";
                DTPlatform.FILE_SEPARATOR = System.getProperty("file.separator");
                outFilePath = outFilePath + DTPlatform.FILE_SEPARATOR + rootPath.split(RQ)[1].replace(DTPlatform.FILE_SEPARATOR,"_").substring(1)+".ccLog";

                correctingCommit(diffClasses,projectName,mutationProviderProject,outFilePath);
            }
        }
    }

    public static List<String> getAllClassFile(File rootFile){
        List<String> result = new ArrayList<>();
        for (File file : rootFile.listFiles()) {
            if (file.isDirectory()) {
                result.addAll(getAllClassFile(file));
            }else {
                if (file.getName().contains(".class")) {
                    result.add(file.getAbsolutePath());
                }
            }
        }
        return result;
    }

    public static void correctingCommit(List<ClassInfo> diffClasses,String projectName,String mutationProviderProject,String outFilePath) throws IOException {
        timeStamp = String.valueOf(new Date().getTime());

        //Generate the difference test log file, set the JVM and project output path, and load the JVM
        DTGlobal.setDiffLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "difference");
        DTGlobal.setSelectLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "selectInfo");
        DTGlobal.setInsertLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "insertInfo");
        DTConfiguration.debug = false;
        DTConfiguration.setJvmDepensRoot("." + DTPlatform.FILE_SEPARATOR + "01JVMS");
        DTConfiguration.setProjectDepensRoot("." + DTPlatform.FILE_SEPARATOR + "sootOutput");
        System.out.println(DTPlatform.getInstance());


        // Generate the folder that holds the history program during execution
        String mutationHistoryPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "classhistory";
        String diffClassPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "diffClass";
        String projectFlag = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + mutationProviderProject +"--"+projectName;
        MainHelper.createFolderIfNotExist(projectFlag);
        MainHelper.createFolderIfNotExist(mutationHistoryPath);
        MainHelper.createFolderIfNotExist(diffClassPath);

        // Load the original project and Mutant output project, initialize the SOOT environment, and perform basic block analysis
        ProjectInfo originProject = null;
        ProjectInfo targetProject = null;
        ProjectInfo originMutationProject = null;
        ProjectInfo mutationProject = null;

        // Load the original project and Mutant output project
        if ( projectName == null || projectName.equals(mutationProviderProject) ){
            // Load the original project and Mutant output project
            crossProject = false;
            originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
            targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);
            System.out.println(originProject);
            System.out.println(targetProject);

            // Initialize the soot environment
            Configuration.initSootEnvWithClassPath(targetProject.getpClassPath());
            Configuration.set_output_path(targetProject.getSrcClassPath());
            // Overwrite the files in SootOutput with files in 02Benchmark
            List<String> seedClasses = originProject.getApplicationClasses();
            MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
            targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
            seeds = MainHelper.initialSeeds(seedClasses, targetProject.getSrcClassPath());

            // Modify the accessibility of variables and methods in a seed program to public
            List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(seedClasses);

            // reinitialize the SOOT environment
            G.reset();
            Configuration.initSootEnvWithClassPath(targetProject.getpClassPath());
            Configuration.set_output_path(targetProject.getSrcClassPath());
        }else {
            crossProject = true;

            originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
            targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);
            originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, providerProjectPreDefineFlag);
            mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, providerProjectPreDefineFlag);
            System.out.println(originProject);
            System.out.println(targetProject);
            System.out.println(originMutationProject);
            System.out.println(mutationProject);

            // Initialize the soot environment
            Configuration.initSootEnvWithClassPath(mutationProject.getpClassPath());
            Configuration.set_output_path(mutationProject.getSrcClassPath());
            // Overwrite the files in SootOutput with files in 02Benchmark
            List<String> originMutateClasses = originMutationProject.getApplicationClasses();
            MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
            mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));
            List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);

            // reinitialize the SOOT environment
            G.reset();
            Scene.v().addBasicClass("java.lang.StringBuilder");
            Scene.v().addBasicClass("java.io.PrintStream");
            Scene.v().addBasicClass("java.lang.System");
            Scene.v().addBasicClass("java.lang.String");
            Scene.v().addBasicClass("com.sun.crypto.provider.Cipher.AEAD.ReadWriteSkip$SkipTest");
            Configuration.initSootEnvWithClassPath(targetProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath());
            Configuration.set_output_path(targetProject.getSrcClassPath());

            // Overwrite the files in SootOutput with files in 02Benchmark
            List<String> seedClasses = originProject.getApplicationClasses();
            MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
            targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
            List<String> seedJunits = originProject.getJunitClasses();
            MainHelper.restoreBadClasses(seedJunits, originProject, targetProject);
            targetProject.setJunitClasses(new ArrayList<>(seedJunits));
            seeds = MainHelper.initialSeedsWithType(seedClasses, targetProject.getSrcClassPath(), false, mutationHistoryPath);
            seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, targetProject.getTestClassPath(), true, mutationHistoryPath));
        }
        diffClasses.sort(new Comparator<ClassInfo>() {
            @Override
            public int compare(ClassInfo o1, ClassInfo o2) {
                String s1 = o1.getClassName().split("@")[1].replace(".class", "");
                String s2 = o2.getClassName().split("@")[1].replace(".class", "");
                return s1.compareTo(s2);
            }
        });
        List<String> correctingCommitResult = new ArrayList<>();
        List<String> uniqueCorrectingCommitId = new ArrayList<>();
        int count=  0;
        for (ClassInfo seed : diffClasses) {
            System.out.print(Math.ceil(((double)count/diffClasses.size())*100)+"/"+100);
            for(int i=0;i<100;i++){
                if(i<((double)count/diffClasses.size())*100){
                    System.out.print("*");
                }else {
                    System.out.print("-");
                }
            }
            System.out.print(count+"/"+diffClasses.size());
            System.out.println();
            count++;

            String originClassPath = seed.getOriginClassPath();
            seed.setOriginClassPath(targetProject.getSrcClassPath()+DTPlatform.FILE_SEPARATOR+originClassPath);
            seed.storeToCoverOriginClass();

            List<String> uniqueChecksum = new ArrayList<>();
            boolean skipFlag = false;
            for (JvmInfo jvmInfo : importantJvmCmd) {
                String result = executeClass(jvmInfo,targetProject,mutationProject,seed.getOriginClassName());
                if(!result.contains("my_check_sum_value:")){
                    skipFlag = true;
                    break;
                }
                if (!uniqueChecksum.contains(result)) {
                    uniqueChecksum.add(result);
                }
            }
            if(skipFlag || uniqueChecksum.size() == 1){
                continue;
            }
            uniqueChecksum.clear();

            StringBuilder correctingCommitId = new StringBuilder();
            for (JvmInfo jvmInfo : allJvmCmd) {
                String result = executeClass(jvmInfo,targetProject,mutationProject,seed.getOriginClassName());
                if(!result.contains("my_check_sum_value:")){
                    skipFlag = true;
                    break;
                }
                if (!uniqueChecksum.contains(result)) {
                    uniqueChecksum.add(result);
                }
                correctingCommitId.append(","+uniqueChecksum.indexOf(result));
                if(uniqueChecksum.size()==allJvmCmd.size()/2){
                    break;
                }
            }
            correctingCommitId = new StringBuilder(correctingCommitId.substring(1));
            if(skipFlag){
                continue;
            }
            System.out.println(correctingCommitId);
            if(uniqueChecksum.size()<allJvmCmd.size()/2 && uniqueChecksum.size()>=2){
                if(!uniqueCorrectingCommitId.contains(correctingCommitId.toString())){
                    uniqueCorrectingCommitId.add(correctingCommitId.toString());
                    correctingCommitResult.add(seed.getClassName());
                }
            }
            File file = new File(outFilePath);
            FileWriter fileWriter = new FileWriter(file);
            for (String className : correctingCommitResult) {
                fileWriter.write(className);
                fileWriter.write("\n");
            }
            fileWriter.flush();
            fileWriter.close();
        }
    }

    public static void getAllJvmInfo(String jvmRootPath){
        for (File developerDir : Objects.requireNonNull(new File(jvmRootPath).listFiles())) {
            for (File versionDir : Objects.requireNonNull(developerDir.listFiles())) {
                for (File file : Objects.requireNonNull(versionDir.listFiles())) {
                    String javaCmd = file.getAbsolutePath()+ DTPlatform.FILE_SEPARATOR+"bin"+DTPlatform.FILE_SEPARATOR+"java";;
                    String jvmName = developerDir.getName();
                    String rootPath =versionDir.getAbsolutePath();
                    String folderName =file.getName();
                    String version = versionDir.getName();
                    JvmInfo jvmInfo = new JvmInfo(rootPath,folderName,jvmName,version,javaCmd);
                    allJvmCmd.add(jvmInfo);
                }
            }
        }
        for (JvmInfo jvmInfo : allJvmCmd) {
            if(jvmInfo.getJavaCmd().contains("Openj9"+DTPlatform.FILE_SEPARATOR+"openjdk8"+DTPlatform.FILE_SEPARATOR+"0.8.0"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Openj9"+DTPlatform.FILE_SEPARATOR+"openjdk8"+DTPlatform.FILE_SEPARATOR+"0.33.1"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Openj9"+DTPlatform.FILE_SEPARATOR+"openjdk11"+DTPlatform.FILE_SEPARATOR+"0.12.1"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Openj9"+DTPlatform.FILE_SEPARATOR+"openjdk11"+DTPlatform.FILE_SEPARATOR+"0.33.1"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Hotspot"+DTPlatform.FILE_SEPARATOR+"openjdk8"+DTPlatform.FILE_SEPARATOR+"8u0"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Hotspot"+DTPlatform.FILE_SEPARATOR+"openjdk8"+DTPlatform.FILE_SEPARATOR+"8u345"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Hotspot"+DTPlatform.FILE_SEPARATOR+"openjdk11"+DTPlatform.FILE_SEPARATOR+"11.0.1"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
            if(jvmInfo.getJavaCmd().contains("Hotspot"+DTPlatform.FILE_SEPARATOR+"openjdk11"+DTPlatform.FILE_SEPARATOR+"11.0.16.1"+DTPlatform.FILE_SEPARATOR)){
                importantJvmCmd.add(jvmInfo);
            }
        }

    }

    public static String executeClass(JvmInfo jvmInfo,
                                      ProjectInfo currentProject,
                                      ProjectInfo mutateProvideProject,
                                      String executeClassName){
        String classPath = currentProject.getpClassPath();
        if (mutateProvideProject != null){
            classPath = classPath + DTPlatform.PATH_SEPARATOR + mutateProvideProject.getpClassPath();
        }

        ArrayList<String> projOptions = currentProject.getProjoptions();
        String[] projOptionsArray = projOptions.toArray(new String[projOptions.size()]);

        String cmdExecute = ExecutorHelper.assembleJavaCmd(jvmInfo.getJavaCmd(), currentProject.getVmoptions(), classPath, executeClassName, false, projOptionsArray);
        System.out.println(cmdExecute);

        CFMExecutor.getInstance().execute(cmdExecute);

        JvmOutput jvmOutput = CFMExecutor.getInstance().getCurrentOutput();
        if (jvmOutput != null){
            if(jvmOutput.getStdout().contains("my_check_sum_value:")){
                jvmOutput.setStdout(jvmOutput.getStdout().substring(jvmOutput.getStdout().indexOf("my_check_sum_value:"))+"\n");
                jvmOutput.setStdout(jvmOutput.getStdout().substring(0,jvmOutput.getStdout().indexOf("\n")));
            }else {
                jvmOutput.setStdout("");
            }
            System.out.println("stdout:" + jvmOutput.getStdout());
            System.out.println("stderr:" + jvmOutput.getStderr());
            return jvmOutput.getStdout();

        } else {
            return "";
        }
    }

}
