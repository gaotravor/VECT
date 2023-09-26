import JimpleMixer.blocks.*;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;

import core.*;

import dtjvms.*;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.CFM.JvmOutput;
import dtjvms.loader.DTLoader;

import org.apache.commons.cli.CommandLine;
import org.junit.Test;
import soot.*;
import soot.options.Options;

import java.io.*;
import java.util.*;

import core.ChecksumHelper;

import static core.MainHelper.coverageTest;
import static core.MainHelper.getIngredientBlockRandom;

/**
 * If you have followed the instructions described in the ReadMe, you can start the entire project here
 */
public class Main {

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

    public static int useClustering = CodeClusterHelper.PL_BART_CLUSTER;
    public static int selectMethod = SelectBlockHelper.RWS_SELECT;
    public static boolean checksum = true;

    public static long exeTime = 60 * 60 * 24;

    public static boolean covTest = false; //JVM coverage collect flag
    public static String covJavaCmd = "/home/share/Fasttailor/jvmCov/openjdk/build/linux-x86_64-normal-server-release/jdk/bin/java"; // the path of JVM that can collect coverage

    public static int randomSeed = 1;
    public static String timeStamp;
    public static List<ClassInfo> seeds;
    public static boolean crossProject = false;

    public static boolean providerProjectAllDefineFlag = false;  // Whether the project extracts all available classes
    public static String packagePath = "out"+DTPlatform.FILE_SEPARATOR+"production"+DTPlatform.FILE_SEPARATOR+"HotspotIssue";  // packagePath + packageName 指向项目的可用类
    public static String packageName = "Bug_triggering_input";

    static {
        Options.v().set_weak_map_structures(true);
    }
    /**
     * Here we go！
     * @param args
     */
    public static void main(String[] args) throws IOException {
        run(args);
    }

    public static void run(String[] args)  throws IOException {

        // Read your Settings from the command line
        argsAnalysis(args);
        if(MainConfiguration.randomSeed == -1){
            MainConfiguration.randomSeed = (int) System.currentTimeMillis();
        }

//        Configuration.randomSeed = MainConfiguration.randomSeed;

        timeStamp = String.valueOf(new Date().getTime());

        //Generate the difference test log file, set the JVM and project output path, and load the JVM
        DTGlobal.setDiffLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "difference");
        DTGlobal.setSelectLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "selectInfo");
        DTGlobal.setInsertLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "insertInfo");
        DTConfiguration.debug = false;
        DTConfiguration.setJvmDepensRoot("." + DTPlatform.FILE_SEPARATOR + "01JVMS");
        DTConfiguration.setProjectDepensRoot("." + DTPlatform.FILE_SEPARATOR + "sootOutput");
        System.out.println(DTPlatform.getInstance());

        // Get JVM information
        ArrayList<JvmInfo> jvmCmds = DTLoader.getInstance().loadJvms();
        for (JvmInfo jvmCmd : jvmCmds) {
            File file = new File(jvmCmd.getRootPath()+DTPlatform.FILE_SEPARATOR+jvmCmd.getFolderName()+DTPlatform.FILE_SEPARATOR+".options");
            if(file.exists()){
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line = bufferedReader.readLine();
                while (line!=null)
                {
                    jvmCmd.setJavaCmd(jvmCmd.getJavaCmd()+" "+line);
                    line = bufferedReader.readLine();
                }
            }
            System.out.println(jvmCmd);
        }


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
        if ( projectName == null || projectName.equals(mutationProviderProject) ){

            // Load the original project and Mutant output project
            crossProject = false;
            originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
            targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);
            System.out.println(originProject);
            System.out.println(targetProject);

            // Initialize the soot environment
            sceneReset(targetProject.getpClassPath(), targetProject.getSrcClassPath());
            // Overwrite the files in SootOutput with files in 02Benchmark
            List<String> seedClasses = originProject.getApplicationClasses();
            MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
            targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
            seeds = MainHelper.initialSeeds(seedClasses, targetProject.getSrcClassPath());

            if(providerProjectAllDefineFlag){
                originProject.setApplicationClasses((ArrayList<String>) ClazzUtils.run("02Benchmarks"+DTPlatform.FILE_SEPARATOR+projectName,packagePath,packageName));
                seedClasses = originProject.getApplicationClasses();
                MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
                targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
            }

            // Modify the accessibility of variables and methods in a seed program to public
            List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(seedClasses);

            // reinitialize the SOOT environment
            sceneReset(targetProject.getpClassPath(), targetProject.getSrcClassPath());

            // Extract ingredients
            BlocksContainer.initMutantsFromClasses(mutationClasses);
        } else {

            // Load the original project and Mutant output project
            crossProject = true;
            originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
            targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);
            originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, providerProjectPreDefineFlag);
            mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, providerProjectPreDefineFlag);
            if(providerProjectAllDefineFlag){
                originMutationProject.setApplicationClasses((ArrayList<String>) ClazzUtils.run("02Benchmarks"+DTPlatform.FILE_SEPARATOR+mutationProviderProject,packagePath,packageName));
            }
            System.out.println(originProject);
            System.out.println(targetProject);
            System.out.println(originMutationProject);
            System.out.println(mutationProject);

            // Initialize the soot environment
            sceneReset(mutationProject.getpClassPath(), mutationProject.getSrcClassPath());
            // Overwrite the files in SootOutput with files in 02Benchmark
            List<String> originMutateClasses = originMutationProject.getApplicationClasses();
            MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
            mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));
            List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);

            // reinitialize the SOOT environment
            sceneReset(targetProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath(), targetProject.getSrcClassPath());
            // Overwrite the files in SootOutput with files in 02Benchmark
            List<String> seedClasses = originProject.getApplicationClasses();
            MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
            targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
            List<String> seedJunits = originProject.getJunitClasses();
            MainHelper.restoreBadClasses(seedJunits, originProject, targetProject);
            targetProject.setJunitClasses(new ArrayList<>(seedJunits));
            seeds = MainHelper.initialSeedsWithType(seedClasses, targetProject.getSrcClassPath(), false, mutationHistoryPath);
            seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, targetProject.getTestClassPath(), true, mutationHistoryPath));

            // Extract ingredients
            BlocksContainer.initMutantsFromClasses(mutationClasses);
        }



        // Special processing
        if(projectName.contains("jython-dacapo")){
            ArrayList<String> s = new ArrayList<>();
            for (String projoption : originProject.getProjoptions()) {
                s.add(projoption.replace("jython-dacapo",projectName));
            }
            originProject.setProjoptions(s);
            targetProject.setProjoptions(s);
        }

        ChecksumHelper.createChecksumFile(targetProject.getSrcClassPath());
        ChecksumHelper.setSkipClass("."+DTPlatform.FILE_SEPARATOR+"02Benchmarks"+DTPlatform.FILE_SEPARATOR+projectName+DTPlatform.FILE_SEPARATOR+"skipclass.txt");

        CodeClusterHelper.clusteringFromBlock(useClustering);

        coverageTest(covTest,covJavaCmd,jvmCmds,targetProject,seeds);

        String classFileFolder = "";
        for(ClassInfo seed :seeds){
            classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
            MainHelper.createFolderIfNotExist(classFileFolder);
        }

        ChecksumHelper.checksumForProject(checksum, seeds, mutationHistoryPath, targetProject);


        Random random = new Random(MainConfiguration.randomSeed);
        int sumDiffFindCount = 0;
        Unit targetUnit = null;
        long originTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        while (seeds.size() > 0){

            System.out.println(seeds.size());
            BlockInfo ingredient = null;
            long endTime = System.currentTimeMillis();
            System.out.print(Math.ceil(((double)endTime - originTime)/exeTime/10));
            System.out.print("/100");
            for (int i=0;i<100;i++){
                if(i<(((double)endTime - originTime)/exeTime/10)){
                    System.out.print("*");
                }else {
                    System.out.print("-");
                }
            }
            System.out.print((endTime-originTime)/1000);
            System.out.print("/");
            System.out.print(exeTime);
            System.out.println();
            // Run time limit
            if(endTime - originTime > 1000 * exeTime){
                File file = new File("03results"+DTPlatform.FILE_SEPARATOR+timeStamp+DTPlatform.FILE_SEPARATOR+projectName+DTPlatform.FILE_SEPARATOR+"DiffAndSelectTime.txt");
                FileWriter fileWriter = new FileWriter(file,true);
                fileWriter.write(new java.util.Date(endTime)+"\n");
                fileWriter.write(sumDiffFindCount+","+SelectBlockHelper.getSelectTime()+"\n");
                fileWriter.flush();
                fileWriter.close();
                return;
            }

            // Record sumDiffFindCount every 20 minutes
            if(endTime - startTime > 20 * 60 * 1000){
                startTime = endTime;
                File file = new File("03results"+DTPlatform.FILE_SEPARATOR+timeStamp+DTPlatform.FILE_SEPARATOR+projectName+DTPlatform.FILE_SEPARATOR+"DiffAndSelectTime.txt");
                FileWriter fileWriter = new FileWriter(file,true);
                fileWriter.write(new java.util.Date(endTime)+"\n");
                fileWriter.write(sumDiffFindCount+","+SelectBlockHelper.getSelectTime()+"\n");
                fileWriter.flush();
                fileWriter.close();
            }

            ClassInfo seed = seeds.get(random.nextInt(seeds.size()));
            classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
            MainHelper.createFolderIfNotExist(classFileFolder);
            System.out.println(seed.getOriginClassName());

            if (seed.isJunit()){
                Configuration.set_output_path(targetProject.getTestClassPath());
            } else {
                Configuration.set_output_path(targetProject.getSrcClassPath());
            }
            if (seed.getMutationTimes() > 10){
                seeds.remove(seed);
                continue;
            }

            System.out.println("current: " + seed.getClassName());

            // Soot class was loaded and converted into Soot method
            SootClass seedClass;
            if (seed.isOriginClass() && !seed.hasCovered()){
                seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            } else {
                seed.storeToCoverOriginClass();
                seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            }

            if (seedClass == null){
                continue;
            }

            Map<String, Body> candidateMethods = new HashMap<>();
            List<SootMethod> seedMethods = seedClass.getMethods();
            // Gets the function body of each method
            for (SootMethod seedMethod : seedMethods) {
                if (!seedMethod.isAbstract()) {
                    try {
                        Body methodBody = seedMethod.retrieveActiveBody();
                        if (!seedMethod.isConstructor()) {
                            candidateMethods.put(seedMethod.getName(), methodBody);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // Get a function body at random
            List<String> tmp = new ArrayList<>(candidateMethods.keySet());
            if (tmp.size() == 0){
                seeds.remove(seed);
                Scene.v().removeClass(seedClass);
                continue;
            }
            Body methodBody = candidateMethods.get(tmp.get(random.nextInt(tmp.size())));

            // Insert a base block at random
            if (methodBody != null){

                List<Unit> validUnits = MainHelper.getValidStmtsFromMethodBody(methodBody);

                targetUnit = MainHelper.getTargetUnitRandom(random, validUnits);

                ingredient = MainHelper.getIngredient(selectMethod,random);
                int tryTimes = 0;
                while (ingredient == null && tryTimes < 10){
                    ingredient = MainHelper.getIngredient(selectMethod,random);
                    tryTimes++;
                }

                if (ingredient != null){

                    String mutantClassName = ingredient.getClassName();
                    List<Local> originLocal = new ArrayList<>(methodBody.getLocals());
                    MutationHelper.insertBlockToMethod(seedClass, methodBody, targetUnit, BlocksContainer.getAllMutationMap().get(mutantClassName), ingredient);

                    boolean skipFlag = false;
                    for (Unit allStmt : ingredient.getAllStmts()) {
                        String[] skipWord = new String[]{"math","bean","time","random","hash","port"};
                        for (String s : skipWord) {
                            if(allStmt.toString().toLowerCase(Locale.ROOT).contains(s)){
                                skipFlag = true;
                            }
                        }
                    }
                    if(!skipFlag){
                        List<Local> mutantLocal = new ArrayList<>(methodBody.getLocals());
                        List<Local> newLocal = new ArrayList<>();
                        for (Local local : mutantLocal) {
                            if(!originLocal.contains(local)){
                                newLocal.add(local);
                            }
                        }
                        if(checksum){
                            try{
                                ChecksumHelper.updateCheckSumStmtAfterLastWrite(seedClass,methodBody,newLocal);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }

                }
            }
            //Save the SootClass local
            if (JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)) {
                seed.mutationTimesIncrease();
                String newName = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        newName,
                        classFileFolder + DTPlatform.FILE_SEPARATOR + newName,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                if (crossProject){
                    if (!CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
                            targetProject,
                            mutationProject,
                            newMutateClass.getOriginClassName(),
                            newMutateClass.getClassName())){
                        seeds.add(newMutateClass);
                    }
                } else {
                    if (!CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
                            targetProject,
                            newMutateClass.getOriginClassName(),
                            newMutateClass.getClassName())){
                        seeds.add(newMutateClass);
                    }
                }
                if (CFMExecutor.getInstance().isDiffFound()){
                    if(!JvmOutput.timeoutFlag){
                        ingredient.diffFoundTimesIncrease();
                        sumDiffFindCount++;

                        DTGlobal.getInsertInfo().clear();

                        JvmOutput.timeoutFlag = false;
                        String diffClassFolder = diffClassPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
                        MainHelper.createFolderIfNotExist(diffClassFolder);
                        newMutateClass.saveSootClassToTargetPath(seedClass, diffClassFolder + DTPlatform.FILE_SEPARATOR + newMutateClass.getClassName());
                    }
                }
            }
            for (SootMethod seedMethod : seedMethods) {
                seedMethod.releaseActiveBody();
            }
            Scene.v().removeClass(seedClass);

        }
    }

    private static void argsAnalysis(String[] args){
        CommandLine options = MainHelper.parseArgs(args);

        if (options != null){
            if (options.hasOption("help")) { // seed class project
                System.out.println("You can use the following parameters:");
                System.out.println("-s or -seed to set seed class project");
                System.out.println("          for example:HotspotTests-Java");
                System.out.println("          default:HotspotTests-Java");
                System.out.println("-p or -provide to set ingredients provide project");
                System.out.println("          for example:HotspotTests-Java");
                System.out.println("          default:HotspotTests-Java");
                System.out.println("-f or -filePredefined to set predefined classes file");
                System.out.println("          for example:testcases.txt");
                System.out.println("          default:testcases.txt");
                System.out.println("-r or -randomSeed to set random seed");
                System.out.println("          for example:-1 or 0 or 1 or 2...");
                System.out.println("          default: -1 (get by System.currentTimeMillis())");
                System.out.println("-sl or -select to set select method");
                System.out.println("          for example:random or rws");
                System.out.println("          default:rws");
                System.out.println("-cl or -cluster to set cluster method");
                System.out.println("          for example:no_cluster or infercode or codebert or codet5 or plbart");
                System.out.println("          default:plbart");
                System.out.println("-ch or -checksum to set open or close checksum");
                System.out.println("          for example:true or false");
                System.out.println("          default:true");
                System.out.println("-et or -exetime to set test time (s)");
                System.out.println("          for example:36000");
                System.out.println("          default:43200");
                System.exit(0);
            }
            if (options.hasOption("t")) { // time stamp
                timeStamp = options.getOptionValue("t");
            } else {
                timeStamp = String.valueOf(new Date().getTime());
            }

            if (options.hasOption("s")) { // seed class project
                projectName = options.getOptionValue("s");
            }

            if (options.hasOption("p")) { // provide project
                mutationProviderProject = options.getOptionValue("p");
            }

            if (options.hasOption("f")) { // predefined classes file
                defineClassesPath = options.getOptionValue("f");
            }

            if (options.hasOption("r")) { // random seed
                MainConfiguration.randomSeed = Integer.parseInt(options.getOptionValue("r"));
            }

            if(options.hasOption("sl")){ // select method
                String s = options.getOptionValue("sl");
                if (s.toLowerCase(Locale.ROOT).equals("random")) {
                    selectMethod = SelectBlockHelper.RANDOM_SELECT;
                }
                if (s.toLowerCase(Locale.ROOT).equals("softmax")) {
                    selectMethod = SelectBlockHelper.SOFTMAX_SELECT;
                }
                if (s.toLowerCase(Locale.ROOT).equals("rws")) {
                    selectMethod = SelectBlockHelper.RWS_SELECT;
                }
            }

            if(options.hasOption("cl")){ // cluster method
                String s = options.getOptionValue("cl");
                if(s.toLowerCase(Locale.ROOT).equals("no_cluster")){
                    useClustering = CodeClusterHelper.NO_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("infercode")){
                    useClustering = CodeClusterHelper.INFER_CODE_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("codebert")){
                    useClustering = CodeClusterHelper.CODE_BERT_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("codet5")){
                    useClustering = CodeClusterHelper.CODE_T5_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("plbart")){
                    useClustering = CodeClusterHelper.PL_BART_CLUSTER;
                }
            }

            if(options.hasOption("v")){ // vm option
                if (options.getOptionValue("v").toLowerCase(Locale.ROOT).equals("true")) {
                    DTGlobal.useVMOptions = true;
                }
                if (options.getOptionValue("v").toLowerCase(Locale.ROOT).equals("false")) {
                    DTGlobal.useVMOptions = false;
                }
            }
            if(options.hasOption("ch")){ // checksum
                if (options.getOptionValue("ch").toLowerCase(Locale.ROOT).equals("true")) {
                    checksum = true;
                }
                if (options.getOptionValue("ch").toLowerCase(Locale.ROOT).equals("false")) {
                    checksum = false;
                }
            }

            if(options.hasOption("re")){
                if (options.getOptionValue("re").toLowerCase(Locale.ROOT).equals("true")) {
                    SelectBlockHelper.reshape = true;
                }
                if (options.getOptionValue("re").toLowerCase(Locale.ROOT).equals("false")) {
                    SelectBlockHelper.reshape = false;
                }
            }

            if(options.hasOption("et")) { // execute time
                exeTime = Long.parseLong(options.getOptionValue("et"));
            }

            if(options.hasOption("cov")){
                if (options.getOptionValue("cov").toLowerCase(Locale.ROOT).equals("true")) {
                    covTest = true;
                }
                if (options.getOptionValue("cov").toLowerCase(Locale.ROOT).equals("false")) {
                    covTest = false;
                }
            }
        }
    }
    private static void sceneReset(String pClassPath, String srcClassPath){
        G.reset();
        Configuration.initSootEnvWithClassPath(pClassPath);
        Configuration.set_output_path(srcClassPath);
        Scene.v().addBasicClass("compiler.types.TestPhiElimination$A",SootClass.HIERARCHY);
        Scene.v().addBasicClass("java.lang.StringBuilder",SootClass.HIERARCHY);
        Scene.v().addBasicClass("java.io.PrintStream",SootClass.HIERARCHY);
        Scene.v().addBasicClass("java.lang.System",SootClass.HIERARCHY);
        Scene.v().addBasicClass("java.lang.String",SootClass.HIERARCHY);
        Scene.v().addBasicClass("com.sun.crypto.provider.Cipher.AEAD.ReadWriteSkip$SkipTest",SootClass.HIERARCHY);
    }
}
