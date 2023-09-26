package core;

import JimpleMixer.blocks.*;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import dtjvms.DTConfiguration;
import dtjvms.DTGlobal;
import dtjvms.DTPlatform;
import dtjvms.ProjectInfo;
import dtjvms.loader.DTLoader;
import org.junit.Test;
import soot.*;
import soot.options.Options;
import org.jboss.windup.decompiler.api.*;
import org.jboss.windup.decompiler.procyon.ProcyonDecompiler;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CodeClusterHelper {



    public static void main(String[] args) throws IOException, InterruptedException {
        String mutationProviderProject = args[0];  // 提供合成块文件的项目
        String rootDirectory = args[1];
        String jadPath = args[2];
        jimple2java(mutationProviderProject, rootDirectory, jadPath);
    }
    public static final int NO_CLUSTER = 1;
    public static final int INFER_CODE_CLUSTER = 2;
    public static final int CODE_BERT_CLUSTER = 3;
    public static final int CODE_T5_CLUSTER = 4;
    public static final int PL_BART_CLUSTER = 5;

    private static String csvFileName = "";
    /**
     * Gao Tianchang：通过method选择聚类方法
     * @param method
     */
    public static void clusteringFromBlock(int method) {
        if (method == NO_CLUSTER){
            clusteringFromBlockByNoCluster();
        }
        else {
            switch (method){
                case INFER_CODE_CLUSTER:
                    csvFileName = "InferCodeAssignment.csv";
                    break;
                case CODE_BERT_CLUSTER:
                    csvFileName = "CodeBERTAssignment.csv";
                    break;
                case CODE_T5_CLUSTER:
                    csvFileName = "CodeT5Assignment.csv";
                    break;
                case PL_BART_CLUSTER:
                    csvFileName = "PlBartAssignment.csv";
                    break;
            }
            clusteringFromBlockByCsv();
        }
    }
    public static void jimple2java(String mutationProviderProject, String rootDirectory, String jadPath) throws IOException, InterruptedException {


        String classFileDir = rootDirectory+DTPlatform.FILE_SEPARATOR+"classFile";
        String javaFileDir = rootDirectory+DTPlatform.FILE_SEPARATOR+"javaFile";
        String csvFileDir = rootDirectory+DTPlatform.FILE_SEPARATOR+"csvFile";
        MainHelper.createFolderIfNotExist(classFileDir);
        MainHelper.createFolderIfNotExist(javaFileDir);
        MainHelper.createFolderIfNotExist(csvFileDir);

        Jimple2Class.run(mutationProviderProject,classFileDir,csvFileDir);

        List<String> nameList = CSVUtils.importStringCsv(new File(csvFileDir+DTPlatform.FILE_SEPARATOR+"className.csv"));
        int count = 0;
        for(String name:nameList){
            System.out.print((int)Math.ceil(((double)count/nameList.size())*100));
            System.out.print("/100");
            for(int i=0;i<100;i++){
                if(i<Math.ceil(((double)count/nameList.size())*100)){
                    System.out.print("*");
                }else {
                    System.out.print("-");
                }
            }
            System.out.print(count);
            System.out.print("/");
            System.out.println(nameList.size());
            count++;

            if(System.getProperty("os.name").equals("Linux")){

                String cmd = jadPath +
                        " -p "+
                        classFileDir+DTPlatform.FILE_SEPARATOR+name;
                System.out.println(cmd);
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor(20, TimeUnit.SECONDS);

                InputStream inputStream = process.getInputStream();
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null ;
                while ((line = inputReader.readLine()) !=  null ){
                    stringBuilder.append(line).append("\n");
                }
                File outFile = new File(javaFileDir+DTPlatform.FILE_SEPARATOR+name.replace(".class",".java"));
                FileWriter fileWriter = new FileWriter(outFile);
                fileWriter.write(stringBuilder.toString());
                fileWriter.close();
            }else if(System.getProperty("os.name").startsWith("Windows")){

                String cmd = "cmd /c "+
                        jadPath +
                        " -p "+
                        classFileDir+DTPlatform.FILE_SEPARATOR+name+
                        " > "+
                        javaFileDir+DTPlatform.FILE_SEPARATOR+name.replace(".class",".java");

                System.out.println(cmd);
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor(20, TimeUnit.SECONDS);

            }

        }
    }

    private static void clusteringFromBlockByNoCluster() {
        BlocksContainer.allClusteringMutation = new ArrayList<>();
        for(String key:BlocksContainer.validMutationMap.keySet()) {
            List<BlockInfo> blockInfos = BlocksContainer.validMutationMap.get(key);
            for (BlockInfo blockInfo : blockInfos) {
                List<BlockInfo> blockInfoList = new ArrayList<>();
                blockInfoList.add(blockInfo);
                BlocksContainer.allClusteringMutation.add(blockInfoList);
            }
        }
    }

    private static void clusteringFromBlockByCsv() {
        BlocksContainer.allClusteringMutation = new ArrayList<>();
        // 临时存储与语句序列对应的blockInfo
        List<BlockInfo> allBlockInfo = new ArrayList<>();
        for(String key:BlocksContainer.validMutationMap.keySet()){
            List<BlockInfo> blockInfos = BlocksContainer.validMutationMap.get(key);
            allBlockInfo.addAll(blockInfos);
        }
        int [] assignments = CSVUtils.importIntCsv(new File("."+ DTPlatform.FILE_SEPARATOR+csvFileName));
        for(int i = 0; i< Arrays.stream(assignments).max().getAsInt(); i++){
            BlocksContainer.allClusteringMutation.add(new ArrayList<>());
        }
        for(int i=0;i<assignments.length;i++){
            BlocksContainer.allClusteringMutation.get(assignments[i] - 1).add(allBlockInfo.get(i));
        }
    }
}

class Jimple2Class {
    public static String run(String mutationProviderProject, String javaFileDir, String csvFileDir) throws IOException {

        String projectName = "templateClass";  // 提供种子文件的项目
        String timeStamp;
        List<ClassInfo> seeds;

        timeStamp = String.valueOf(new Date().getTime());
        DTConfiguration.debug = false;
        DTConfiguration.setJvmDepensRoot("." + DTPlatform.FILE_SEPARATOR + "01JVMS");
        DTConfiguration.setProjectDepensRoot("." + DTPlatform.FILE_SEPARATOR + "sootOutput");
        ProjectInfo originMutationProject = null;
        ProjectInfo mutationProject = null;

        String mutationHistoryPath = javaFileDir;
        MainHelper.createFolderIfNotExist(mutationHistoryPath);
        Files.walkFileTree(Paths.get(mutationHistoryPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                                                      IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        // 加载ingredients项目
        originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, true);
        mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, true);
        System.out.println(originMutationProject);
        System.out.println(mutationProject);
        // 设置soot环境
        Configuration.initSootEnvWithClassPath(mutationProject.getpClassPath());
        Configuration.set_output_path(mutationProject.getSrcClassPath());
        // 更新ingredients项目
        List<String> originMutateClasses = originMutationProject.getApplicationClasses();
        MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
        mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));
        // 修改ingredients可访问性
        List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);
        // 加载seedclass项目
        ProjectInfo originProject = null;
        ProjectInfo targetProject = null;
        originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, false);
        targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, false);
        System.out.println(originProject);
        System.out.println(targetProject);
        // 设置soot环境
        G.reset();
        Configuration.initSootEnvWithClassPath(targetProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath());
        Configuration.set_output_path(targetProject.getSrcClassPath());
        // 更新seedclass项目
        List<String> seedClasses = originProject.getApplicationClasses();
        MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
        targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
        List<String> seedJunits = originProject.getJunitClasses();
        MainHelper.restoreBadClasses(seedJunits, originProject, targetProject);
        targetProject.setJunitClasses(new ArrayList<>(seedJunits));
        // 生成ingredients
        BlocksContainer.initMutantsFromClasses(mutationClasses);
        // 加载seed class
        seeds = MainHelper.initialSeedsWithType(seedClasses, targetProject.getSrcClassPath(), false, mutationHistoryPath);
        seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, targetProject.getTestClassPath(), true, mutationHistoryPath));

        ClassInfo seed = null;
        for (ClassInfo s : seeds) {
            if(s.getOriginClassName().equals("template")){
                seed = s;
                break;
            }
        }

        assert seed != null;
        String classFileFolder = mutationHistoryPath;
        MainHelper.createFolderIfNotExist(classFileFolder);
        if (seed.isJunit()){
            Configuration.set_output_path(targetProject.getTestClassPath());
        } else {
            Configuration.set_output_path(targetProject.getSrcClassPath());
        }
        Map<String, List<BlockInfo>> ingredients = BlocksContainer.getValidMutationMap();
        Set<String> classCandidate = ingredients.keySet();
        List<StringBuilder> nameList = new ArrayList<>();
        int ingredientSize = 0;
        for(String candidate:classCandidate){
            ingredientSize += ingredients.get(candidate).size();

        }
        int count = 0;
        for(String candidate:classCandidate){
            List<BlockInfo> methodCandidate = ingredients.get(candidate);
            for(BlockInfo ingredient: methodCandidate){

                System.out.print((int)Math.ceil(((double)count/ingredientSize)*100));
                System.out.print("/100");
                for(int i=0;i<100;i++){
                    if(i<Math.ceil(((double)count/ingredientSize)*100)){
                        System.out.print("*");
                    }else {
                        System.out.print("-");
                    }
                }
                System.out.print(count);
                System.out.print("/");
                System.out.println(ingredientSize);
                count++;

                SootClass seedClass;
                if (!seed.isOriginClass() || seed.hasCovered()) {
                    seed.storeToCoverOriginClass();
                }
                seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
                assert seedClass != null;
                for (SootMethod method : seedClass.getMethods()) {
                    if (!method.isAbstract()){
                        method.retrieveActiveBody();
                    }

                }

                SootMethod seedMethod = seedClass.getMethod("void main(java.lang.String[])");
                Body methodBody = seedMethod.retrieveActiveBody();
                List<Unit> validUnits = MainHelper.getValidStmtsFromMethodBody(methodBody);
                Unit targetUnit = validUnits.get(0);
                String mutantClass = ingredient.getClassName();
                MutationHelper.insertBlockToMethod(seedClass, methodBody, targetUnit, BlocksContainer.getAllMutationMap().get(mutantClass), ingredient);
                JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class);
                seed.mutationTimesIncrease();
                String mutateClassFilename = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        seed.generateMutateClassFilename(),
                        classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                nameList.add(new StringBuilder(mutateClassFilename));
                Scene.v().removeClass(seedClass);
            }
        }
        CSVUtils.exportCsv(new File(csvFileDir+DTPlatform.FILE_SEPARATOR+"className.csv"),nameList);
        return classFileFolder;
    }
}