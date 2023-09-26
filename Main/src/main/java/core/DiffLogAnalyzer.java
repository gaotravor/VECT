package core;

import dtjvms.DTPlatform;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 *
 * 概念说明
 * rootPath:包含子文件夹 03results 的根路径
 * path:指向一个确定的 diffrence.log 文件的路径
 * diffLog:读取 diffrence.log 后的 File 类型文件
 * diff:一个 diff 的全部内容
 * diffDetail:将一个diff拆分为 className  crashMess  [JVMInfo  output] * n 的格式
 */
public class DiffLogAnalyzer {
    public static void main(String[] args) throws IOException {
        String rootPath = args[0];
        File listFile = new File(rootPath);

        new File(listFile.getAbsolutePath()+DTPlatform.FILE_SEPARATOR+"checksum.txt").delete();
        new File(listFile.getAbsolutePath()+DTPlatform.FILE_SEPARATOR+"uniqueCrash.txt").delete();

        File diffLog;

        diffLog = DiffLogAnalyzer.getFile(listFile.getAbsolutePath()+DTPlatform.FILE_SEPARATOR+"difference.log");


        List<StringBuilder> diffs = DiffLogAnalyzer.clearTimeOutDiff(DiffLogAnalyzer.getInfoForEachDiff(diffLog));



        int crashCount = 0;
        int checkSumCount = 0;
        int skipSize = 0;
        Set<String> uniqueCrash = new HashSet<>();
        Set<String> uniqueCheck = new HashSet<>();
        for (StringBuilder diff : diffs) {
            List<StringBuilder> diffDetail = DiffLogAnalyzer.getDetailForDiff(diff);
            String crashMess = diffDetail.get(1).toString();

            crashMess = crashMess + " ";

            boolean isCrash = true;
            if(diff.toString().contains("Normal Output Inconsistent:")){
                isCrash = false;
            }

            if(isCrash){
                crashCount++;
                if(!uniqueCrash.contains(crashMess)){
                    uniqueCrash.add(crashMess);
                    File uniqueCrashFile = new File(listFile.getAbsolutePath()+DTPlatform.FILE_SEPARATOR+"uniqueCrash.txt");
                    FileWriter fileWriter = new FileWriter(uniqueCrashFile,true);
                    fileWriter.write(diffDetail.get(0).toString()+"\n");
                    fileWriter.write(crashMess+"\n");
                    fileWriter.flush();
                    fileWriter.close();
                }
            }else {
                checkSumCount++;
                if(!uniqueCheck.contains(crashMess)){
                    uniqueCheck.add(crashMess);
                    File uniqueChecksumName = new File(listFile.getAbsolutePath()+DTPlatform.FILE_SEPARATOR+"checksum.txt");
                    FileWriter fileWriter = new FileWriter(uniqueChecksumName,true);
                    fileWriter.write(diffDetail.get(0).toString()+"\n");
                    fileWriter.write(crashMess+"\n");
                    fileWriter.flush();
                    fileWriter.close();
                }
            }


        }

        System.out.println("allCrash:"+crashCount);
        System.out.println("uniqueCrash:"+uniqueCrash.size());
        System.out.println("allChecksum:"+checkSumCount);
        System.out.println("uniqueChecksum:"+uniqueCheck.size());



    }

    public static int clearBug(String crashMess){
        String hotspot_old = crashMess.split("hotspot_old \\[")[1].split("]")[0];
        String hotspot = crashMess.split("hotspot \\[")[1].split("]")[0];
        String openj9_old = crashMess.split("openj9_old \\[")[1].split("]")[0];
        String openj9 = crashMess.split("openj9 \\[")[1].split("]")[0];

        Set<String> crash4Jvm = new HashSet<>();
        crash4Jvm.add(hotspot_old);
        crash4Jvm.add(openj9_old);
        if(crashMess.contains("bisheng")){
            String bisheng_old = crashMess.split("bisheng_old \\[")[1].split("]")[0];
            String bisheng = crashMess.split("bisheng \\[")[1].split("]")[0];
            crash4Jvm.add(bisheng_old);
        }

        return crash4Jvm.size();
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
    /**
     * 指定单个diffLog的位置，返回文件
     * @param path
     * @return
     */
    public static File getFile(String path){
        return new File(path);
    }

    /**
     * 合并 rootPath 下 03results 文件夹中的所有 diffLog，重新保存在 rootPath 下
     * @param rootPath
     * @return
     * @throws IOException
     */
    public static File getAllFile(String rootPath) throws IOException {
        File results = new File(rootPath+"\\03results");
        File allDiff = new File(rootPath+"\\difference.log");
        File allExeTime = new File(rootPath+"\\DiffAndSelectTime.txt");
        allDiff.delete();
        allDiff.createNewFile();
        allExeTime.delete();
        allExeTime.createNewFile();
        FileWriter fileWriter = new FileWriter(allDiff,true);
        FileWriter exeTimeWriter = new FileWriter(allExeTime,true);
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

            File inputFile = new File(Objects.requireNonNull(timeStamp.listFiles())[0].getAbsolutePath()+"\\DiffAndSelectTime.txt");
            bufferedReader = new BufferedReader(new FileReader(inputFile));
            line = bufferedReader.readLine();
            while(line!=null){
                exeTimeWriter.write(line+"\n");
                line=bufferedReader.readLine();
            }
        }

        fileWriter.flush();
        fileWriter.close();
        exeTimeWriter.flush();
        exeTimeWriter.close();
//        File allDiff = new File(rootPath+"\\exceptionUniqueFile.txt");
        return allDiff;
    }

    /**
     * 将 diffLog 文件中的每一个 diff 提取出来
     * @param diffLog
     * @return
     * @throws IOException
     */
    public static List<StringBuilder> getInfoForEachDiff(File diffLog) throws IOException {
        List<StringBuilder> diffs = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(diffLog));
        String line = bufferedReader.readLine();
        while (line != null){
            if(line.contains("Difference found:")){
                diffs.add(new StringBuilder(""));
            }
            diffs.get(diffs.size() - 1).append(line).append("\n");
            line = bufferedReader.readLine();
        }
        return diffs;
    }

    /**
     * 将一个 diff 拆分为 className  crashMess  [JVMInfo  output] * n 的格式
     * @param diff
     * @return
     */
    public static List<StringBuilder> getDetailForDiff(StringBuilder diff){
        List<StringBuilder> diffDetail = new ArrayList<>(); // className  crashMess  [JVMInfo  output] * n

        String[] lines = diff.toString().split("\n");
        String[] temp = lines[0].split("-");
        diffDetail.add(new StringBuilder(temp[temp.length - 1]));
        diffDetail.add(new StringBuilder(lines[1]));
        for (int i = 2;i< lines.length;i++){
            String line = lines[i];
            if(line.contains("======")){
                line = line.replace("=","");
                diffDetail.add(new StringBuilder(line));
                diffDetail.add(new StringBuilder());
                continue;
            }
            if(line.contains("Target") || line.equals("")){
                continue;
            }
            diffDetail.get(diffDetail.size() - 1).append(line).append("\n");
        }
        return diffDetail;
    }

    /**
     * 删除所有 diff 记录中的 TimeOut 类型的差异
     * @param diffs
     * @return
     */
    public static List<StringBuilder> clearTimeOutDiff(List<StringBuilder> diffs){
        List<StringBuilder> noTimeoutDiffs = new ArrayList<>();
        for (StringBuilder diff : diffs) {
            if(!(diff.toString().contains("TIMEOUT"))){
                noTimeoutDiffs.add(diff);
            }
        }
        return noTimeoutDiffs;
    }
//    ||diff.toString().contains("Thread-")||diff.toString().contains("OutOfMemory")||diff.toString().contains("Not enough space")





}
