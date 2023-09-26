package core;

import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.core.JMUtils;
import dtjvms.DTConfiguration;
import dtjvms.DTPlatform;
import dtjvms.JvmInfo;
import dtjvms.ProjectInfo;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.ExecutorHelper;
import org.apache.commons.cli.*;
import soot.*;
import soot.jimple.internal.JIdentityStmt;
import soot.tagkit.AbstractHost;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This is a helper class for controls Main and contains some helper functions
 */
public class MainHelper {

    public static Options options = null;
    /**
     * Bind parameters to the Option variable
     */
    public static void loadOptions(){

        options = new Options();
        options.addOption(new Option("help","help",false,"help"));
        options.addOption(new Option("t", "timestamp", true, "time stamp"));
        options.addOption(new Option("s", "seed", true, "seed class project"));
        options.addOption(new Option("p", "provide", true, "provide project"));
        options.addOption(new Option("f", "filePredefined", true, "predefined classes file"));
        options.addOption(new Option("r", "randomSeed", true, "random seed"));
        options.addOption(new Option("sl", "select", true, "select method"));
        options.addOption(new Option("cl", "cluster", true, "cluster method"));
        options.addOption(new Option("v", "vmoption", true, "vm option"));
        options.addOption(new Option("ch", "checksum", true, "checksum"));
        options.addOption(new Option("l", "loop", true, "loop wrap"));
        options.addOption(new Option("et", "exetime", true, "execute time"));
        options.addOption(new Option("re","reshape",true,"reshape cluster"));
        options.addOption(new Option("cov","coverage",true,"JVM coverage collect"));
    }

    /**
     * 将 covJavaCmd 添加到 jvmCmds 当中，并且执行 targetProject 的所有种子文件 seeds
     * @param covJavaCmd
     * @param jvmCmds
     * @param targetProject
     * @param seeds
     */
    public static void coverageTest(boolean covTest, String covJavaCmd,ArrayList<JvmInfo> jvmCmds,ProjectInfo targetProject,List<ClassInfo> seeds){
        if(!covTest){
            return;
        }
        JvmInfo jvmInfo = new JvmInfo("","","jvmCoverage","",covJavaCmd);
        jvmInfo.setVmOptions(null);
        jvmCmds.add(jvmInfo);


        for (ClassInfo seed : seeds) {
            ArrayList<String> projOptions = targetProject.getProjoptions();
            String cmdExecute = ExecutorHelper.assembleJavaCmd(covJavaCmd, targetProject.getVmoptions(), targetProject.getpClassPath(), seed.getOriginClassName(), false, projOptions.toArray(new String[projOptions.size()]));
            System.out.println(cmdExecute);
            Thread ctester = new Thread(new Runnable() {
                @Override
                public void run() {
                    CFMExecutor.getInstance().execute(cmdExecute);
                }
            });
            ctester.start();
            try {
                TimeUnit.SECONDS.timedJoin(ctester, DTConfiguration.CLASS_MAX_RUNTIME);
                CFMExecutor.getInstance().shutDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        System.exit(0);
    }
    /**
     * Format the CMD directive into a CommandLine object
     * @param args
     * @return
     */
    public static CommandLine parseArgs(String[] args){

        if (options == null){
            loadOptions();
        }
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (cmd == null){

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "CFMutation Commands", options );
            System.out.println();
            return null;
        }
        return cmd;
    }

    /**
     *Initialization class information as the initial seed file,
     * The Type's mean is whether it is a Junit class
     * @param classes
     * @param srcClassPath
     * @param isJunit
     * @param bakPath
     * @return
     */
    public static List<ClassInfo> initialSeedsWithType(List<String> classes, String srcClassPath, boolean isJunit, String bakPath) {

        List<ClassInfo> classInfos = new ArrayList<>();
        classes.forEach(clazz -> {

            String cpath = srcClassPath + DTPlatform.FILE_SEPARATOR +
                    clazz.replace(".", DTPlatform.FILE_SEPARATOR) + ".class";
            if (!bakPath.equals("")){

                String classFileFolder = bakPath + DTPlatform.FILE_SEPARATOR + clazz;
                MainHelper.createFolderIfNotExist(classFileFolder);
                String originClassBakPath = classFileFolder + DTPlatform.FILE_SEPARATOR + clazz + "-origin.class";
                MainHelper.copyToFile(cpath, originClassBakPath);
                classInfos.add(new ClassInfo(clazz, cpath, clazz, originClassBakPath , isJunit, 0, 0,false));

            } else {
                classInfos.add(new ClassInfo(clazz, cpath, clazz, cpath, isJunit, 0, 0,false));
            }
        });
        return classInfos;
    }

    /**
     * Initialization class information as the initial seed file
     * @param classes
     * @param srcClassPath
     * @return
     */
    public static List<ClassInfo> initialSeeds(List<String> classes, String srcClassPath) {
        List<ClassInfo> classInfos = new ArrayList<>();
        classes.forEach(clazz -> {

            String opath = srcClassPath + DTPlatform.FILE_SEPARATOR +
                    clazz.replace(".", DTPlatform.FILE_SEPARATOR) + ".class";
            String tpath = opath.replace("sootOutput", "02Benchmarks");
            classInfos.add(new ClassInfo(clazz, opath, clazz, tpath, 0, 0,false));

        });
        return classInfos;
    }

    /**
     * Create a folder based on the path without overwriting it
     * @param folderPath
     */
    public static void createFolderIfNotExist(String folderPath){

        File folder = new File(folderPath);
        if (!folder.exists()){

            folder.mkdirs();
        }
    }

    /**
     *Removes the Identity statement from the function body
     * @param methodBody
     * @return
     */
    public static List<Unit> getValidStmtsFromMethodBody(Body methodBody){

        List<Unit> validUnits = new ArrayList<>();
        for (Unit unit : methodBody.getUnits()) {
            if (! (unit instanceof JIdentityStmt)){
                validUnits.add(unit);
            }
        }
        return validUnits;
    }

    /**
     * Get a random Unit from the available units
     * @param random
     * @param validUnits
     * @return
     */
    public static Unit getTargetUnitRandom(Random random, List<Unit> validUnits){

        Unit targetUnit = null;
        if (validUnits.size() > 0){
            targetUnit = validUnits.get(random.nextInt(validUnits.size()));
        }
        return targetUnit;
    }

    public static String getIngredientClassRandom(Random random, List<String> mutantClasses){

        return mutantClasses.get(random.nextInt(mutantClasses.size()));
    }

    public static BlockInfo getIngredientBlockRandom(Random random, List<BlockInfo> ingredients){

        if (ingredients.size() > 0){
            return ingredients.get(random.nextInt(ingredients.size()));
        }
        return null;
    }

    /**
     * Gao Tianchang：通过method字符串选择选择方法
     * @param method
     * @param random
     * @return
     */
    public static BlockInfo getIngredient(int method,Random random){

           return SelectBlockHelper.getIngredient(method,random);
    }

    public static List<String> loadBadClasses(boolean isJunit){

        List<String> badClasses = new ArrayList<>();
        String tmpPath = "";
        if (isJunit){
            tmpPath = "./tmp-junit.txt";
        } else {
            tmpPath = "./tmp-application.txt";
        }
        File file = new File(tmpPath);
        if (file.exists()){
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.isEmpty()){
                        badClasses.add(line);
                    }
                }
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Exception found when reading: " + tmpPath + " !");
            }
        } else {
//            throw new RuntimeException("Defined Classes Path: " + tmpPath + " not Available!");
        }
        return badClasses;
    }

    /**
     * When the environment is initialized,
     * there may be badClasses left over from the last run in sootClass,
     * so we use this function to override them
     * @param badClasses
     * @param originProject
     * @param targetProject
     */
    public static void restoreBadClasses(List<String> badClasses, ProjectInfo originProject, ProjectInfo targetProject){

        String originAppOutPath = originProject.getSrcClassPath();
        String targetAppOutPath = targetProject.getSrcClassPath();
        String originTestOutPath = originProject.getTestClassPath();
        String targetTestOutPath = targetProject.getTestClassPath();

        for (String badClass : badClasses) {

            String cpath = badClass.replace(".", DTPlatform.FILE_SEPARATOR) + ".class";

            if (originProject.getApplicationClasses().contains(badClass)){

                String sourceFilePath = originAppOutPath + DTPlatform.FILE_SEPARATOR + cpath;
                String targetFilePath = targetAppOutPath + DTPlatform.FILE_SEPARATOR + cpath;
                copyToFile(sourceFilePath, targetFilePath);
            }
            if (originProject.getJunitClasses().contains(badClass)){

                String sourceFilePath = originTestOutPath + DTPlatform.FILE_SEPARATOR + cpath;
                String targetFilePath = targetTestOutPath + DTPlatform.FILE_SEPARATOR + cpath;
                copyToFile(sourceFilePath, targetFilePath);
            }
        }
    }

    /**
     * Copy the contents of the source file to the target file
     * @param sourceFilePath
     * @param targetFilePath
     */
    public static void copyToFile(String sourceFilePath, String targetFilePath){

        try {
            File sourceFile = new File(sourceFilePath);
            if (sourceFile.exists()){
                File targetFile = new File(targetFilePath);
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * To be able to access methods, make all non-public methods to public
     * @param classes
     * @return
     */
    public static List<String> duplicateSeedsAndChangeModifiers(List<String> classes) {

        List<String> seeds = new ArrayList<>();
        for (String aClass : classes) {
            SootClass sclass = JMUtils.loadTargetClass(aClass);
            if (!isPublic(sclass)){
                sclass.setModifiers(sclass.getModifiers() | Modifier.PUBLIC);
            }
            sclass.getFields().forEach(sootField -> {

                if (!isPublic(sootField)){
                    sootField.setModifiers(sootField.getModifiers() | Modifier.PUBLIC);
                }
            });



            for(soot.SootMethod sootMethod:sclass.getMethods()){
                if(sootMethod.isPhantom()){
                    continue;
                }
                sootMethod.retrieveActiveBody();
                if (!isPublic(sootMethod) && !sootMethod.isStaticInitializer() && !sootMethod.isConstructor()){
                    sootMethod.setModifiers(sootMethod.getModifiers() | Modifier.PUBLIC);
                }
            }

//            sclass.setName(aClass + "_mutator");
            sclass.setName(aClass);
            if (JMUtils.saveSootClassToLocal(sclass, soot.options.Options.output_format_class)) {
                seeds.add(aClass);
            }
            for (SootMethod method : sclass.getMethods()) {
                method.releaseActiveBody();
            }
            Scene.v().removeClass(sclass);
        }
        return seeds;
    }

    public static boolean isPublic(AbstractHost node){

        if (node instanceof SootClass){

            SootClass clazz = (SootClass) node;
            if (clazz.isPrivate() || clazz.isProtected() || clazz.getModifiers() == Modifier.STATIC || clazz.getModifiers() == 0){
                return false;
            }
        }
        if (node instanceof SootField){

            SootField field = (SootField) node;
            if (field.isPrivate() || field.isProtected() || field.getModifiers() == Modifier.STATIC || field.getModifiers() == 0){
                return false;
            }
        }
        if (node instanceof SootMethod){

            SootMethod method = (SootMethod) node;
            if (method.isPrivate() || method.isProtected() || method.getModifiers() == Modifier.STATIC || method.getModifiers() == 0){
                return false;
            }
        }
        return true;
    }
}
