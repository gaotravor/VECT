package core;

import dtjvms.DTPlatform;
import dtjvms.ProjectInfo;
import org.junit.Test;
import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
/*
这是一个存储类文件信息的类
包括：
    初始类名字
    初始类路径
    现在的类名
    现在的类路径
    是不是Junit
    变异指令
    变异次数
 */
public class ClassInfo {

    private String originClassName;
    private String originClassPath;
    private String className;
    private String pathToClass;
    private boolean isJunit;
    private int mutationOrder;
    private int mutationTimes;
    private boolean isLoop;

    public ClassInfo() {
    }

    public ClassInfo(String originClassName, String className, int mutationOrder, int mutationTimes) {
        this.originClassName = originClassName;
        this.className = className;
        this.isJunit = false;
        this.mutationOrder = mutationOrder;
        this.mutationTimes = mutationTimes;
        this.isLoop = false;
    }

    public ClassInfo(String seedPath, ProjectInfo targetProject){
        String seedName = seedPath.split("\\\\")[seedPath.split("\\\\").length - 1];
        originClassName = seedName.substring(0,seedName.lastIndexOf("_"));
        originClassPath = targetProject.getSrcClassPath()+ DTPlatform.FILE_SEPARATOR+seedName.substring(0,seedName.lastIndexOf("_")).replace(".",DTPlatform.FILE_SEPARATOR)+".class";
        className = seedName;
        pathToClass = seedPath;
        isJunit = false;
        mutationOrder = Integer.parseInt(seedName.split("_")[1].split("@")[0].substring(0,2));
        mutationTimes = Integer.parseInt(seedName.split("_")[1].split("@")[0].substring(2));
    }



    public ClassInfo(String originClassName, String className, String pathToClass, int mutationOrder, int mutationTimes) {
        this.originClassName = originClassName;
        this.className = className;
        this.pathToClass = pathToClass;
        this.isJunit = false;
        this.mutationOrder = mutationOrder;
        this.mutationTimes = mutationTimes;
        this.isLoop = false;
    }

    public ClassInfo(String originClassName, String originClassPath, String className, String pathToClass, int mutationOrder, int mutationTimes,boolean isLoop) {
        this.originClassName = originClassName;
        this.originClassPath = originClassPath;
        this.className = className;
        this.pathToClass = pathToClass;
        this.isJunit = false;
        this.mutationOrder = mutationOrder;
        this.mutationTimes = mutationTimes;
        this.isLoop = isLoop;
    }

    public ClassInfo(String originClassName, String originClassPath, String className, String pathToClass, Boolean isJunit ,int mutationOrder, int mutationTimes,boolean isLoop) {
        this.originClassName = originClassName;
        this.originClassPath = originClassPath;
        this.className = className;
        this.pathToClass = pathToClass;
        this.isJunit = isJunit;
        this.mutationOrder = mutationOrder;
        this.mutationTimes = mutationTimes;
        this.isLoop = isLoop;
    }

    /**
     * That's whether it's mutated or not
     * @return
     */
    public boolean hasCovered(){
        return mutationTimes != 0;
    }

    public boolean isOriginClass(){
        return originClassName.equals(className);
    }

    /**
     * Override the original class with the current class
     */
    public void storeToCoverOriginClass() {

        try {
            File sourceFile = new File(pathToClass);
            File targetFile = new File(originClassPath);
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOriginClassPath() {
        return originClassPath;
    }

    public void setOriginClassPath(String originClassPath) {
        this.originClassPath = originClassPath;
    }

    public String getOriginClassName() {
        return originClassName;
    }

    public String generateMutateClassFilename(){

        return originClassName + "_" +
                String.format("%02d", mutationOrder + 1) +
                String.format("%02d", mutationTimes) +
                "@" +
                System.currentTimeMillis() +
                ".class";
    }

    /**
     * Save the SootClass to a file
     * @param seedClass
     */
    public boolean saveSootClassToFile(SootClass seedClass) {

        try{

            OutputStream jimpleStreamOut = new FileOutputStream(pathToClass.replace(".class", ".jimple"));
            PrintWriter jimpleWriterOut = new PrintWriter(new OutputStreamWriter(jimpleStreamOut));
            Scene.v().forceResolve("java.io.PrintStream",SootClass.BODIES);
            Scene.v().forceResolve("vm.compiler.share.Random",SootClass.BODIES);
            Scene.v().forceResolve("com.sun.crypto.provider.Cipher.AEAD.ReadWriteSkip$SkipTest",SootClass.BODIES);
            Printer.v().printTo(seedClass, jimpleWriterOut);
            jimpleStreamOut.flush();
            jimpleWriterOut.flush();
            jimpleStreamOut.close();

            OutputStream classStreamOut = new FileOutputStream(pathToClass);
            BafASMBackend backend = new BafASMBackend(seedClass, Options.v().java_version());
            backend.generateClassFile(classStreamOut);
            classStreamOut.flush();
            classStreamOut.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Save the SootClass to the specified path file
     * @param seedClass
     * @param path
     */
    public void saveSootClassToTargetPath(SootClass seedClass, String path) {

        try{
            OutputStream jimpleStreamOut = new FileOutputStream(path.replace(".class", ".jimple"));
            PrintWriter jimpleWriterOut = new PrintWriter(new OutputStreamWriter(jimpleStreamOut));
            Printer.v().printTo(seedClass, jimpleWriterOut);
            jimpleStreamOut.flush();
            jimpleWriterOut.flush();
            jimpleStreamOut.close();

            OutputStream classStreamOut = new FileOutputStream(path);
            BafASMBackend backend = new BafASMBackend(seedClass, Options.v().java_version());
            backend.generateClassFile(classStreamOut);
            classStreamOut.flush();
            classStreamOut.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void mutationTimesIncrease(){

        this.mutationTimes++;
    }

    public boolean isJunit() {
        return isJunit;
    }

    public void setJunit(boolean junit) {
        isJunit = junit;
    }

    public void setOriginClassName(String originClassName) {
        this.originClassName = originClassName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPathToClass() {
        return pathToClass;
    }

    public void setPathToClass(String pathToClass) {
        this.pathToClass = pathToClass;
    }

    public int getMutationOrder() {
        return mutationOrder;
    }

    public void setMutationOrder(int mutationOrder) {
        this.mutationOrder = mutationOrder;
    }

    public int getMutationTimes() {
        return mutationTimes;
    }

    public void setMutationTimes(int mutationTimes) {
        this.mutationTimes = mutationTimes;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public void setLoop(boolean loop) {
        isLoop = loop;
    }
}
