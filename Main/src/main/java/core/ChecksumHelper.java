package core;

import JimpleMixer.core.JMUtils;
import dtjvms.DTPlatform;
import dtjvms.ProjectInfo;
import fj.P;
import org.junit.Test;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.options.Options;
import soot.util.Chain;

import java.io.*;
import java.util.*;

public class ChecksumHelper {
    // checksum 字节码
    private static final byte[] bytes = new byte[]{
            -54,-2,-70,-66,0,0,0,52,0,-114,10,0,27,0,75,7,0,76,8,0,77,8,0,78,8,0,79,8,0,80,8,0,81,8,0,82,8,0,83,9,0,84,0,85,10,0,2,0,86,10,
            0,2,0,87,7,0,88,7,0,89,10,0,90,0,91,10,0,27,0,92,10,0,2,0,93,10,0,94,0,95,10,0,96,0,97,10,0,98,0,99,10,0,100,0,101,10,0,102,0,103,
            10,0,104,0,105,10,0,106,0,107,10,0,108,0,109,7,0,110,7,0,111,1,0,6,60,105,110,105,116,62,1,0,3,40,41,86,1,0,4,67,111,100,101,1,0,15,76,105,110,101,
            78,117,109,98,101,114,84,97,98,108,101,1,0,18,76,111,99,97,108,86,97,114,105,97,98,108,101,84,97,98,108,101,1,0,4,116,104,105,115,1,0,20,76,99,104,101,99,107,115,117,
            109,47,99,104,101,99,107,95,115,117,109,59,1,0,3,114,117,110,1,0,22,40,73,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,41,73,1,0,6,102,105,108,116,
            101,114,1,0,18,76,106,97,118,97,47,108,97,110,103,47,83,116,114,105,110,103,59,1,0,7,102,105,108,116,101,114,115,1,0,19,91,76,106,97,118,97,47,108,97,110,103,47,83,116,
            114,105,110,103,59,1,0,1,101,1,0,21,76,106,97,118,97,47,108,97,110,103,47,69,120,99,101,112,116,105,111,110,59,1,0,7,108,97,115,116,83,117,109,1,0,1,73,1,0,1,
            111,1,0,18,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,13,83,116,97,99,107,77,97,112,84,97,98,108,101,7,0,111,7,0,40,7,0,89,1,0,5,
            40,73,66,41,73,1,0,1,98,1,0,1,66,1,0,5,40,73,83,41,73,1,0,1,115,1,0,1,83,1,0,5,40,73,73,41,73,1,0,1,105,1,0,5,40,73,74,41,73,1,
            0,1,108,1,0,1,74,1,0,5,40,73,70,41,73,1,0,1,102,1,0,1,70,1,0,5,40,73,68,41,73,1,0,1,100,1,0,1,68,1,0,5,40,73,67,41,73,1,0,1,
            99,1,0,1,67,1,0,5,40,73,90,41,73,1,0,1,90,1,0,10,83,111,117,114,99,101,70,105,108,101,1,0,14,99,104,101,99,107,95,115,117,109,46,106,97,118,97,12,0,28,
            0,29,1,0,16,106,97,118,97,47,108,97,110,103,47,83,116,114,105,110,103,1,0,4,116,105,109,101,1,0,9,101,120,99,101,112,116,105,111,110,1,0,5,101,114,114,111,114,1,0,
            7,102,97,105,108,117,114,101,1,0,3,106,100,107,1,0,3,106,114,101,1,0,6,115,121,115,116,101,109,7,0,112,12,0,113,0,114,12,0,115,0,116,12,0,117,0,118,1,0,18,
            106,97,118,97,47,108,97,110,103,47,82,117,110,110,97,98,108,101,1,0,19,106,97,118,97,47,108,97,110,103,47,69,120,99,101,112,116,105,111,110,7,0,119,12,0,120,0,121,12,0,
            122,0,123,12,0,124,0,125,7,0,126,12,0,124,0,127,7,0,-128,12,0,124,0,-127,7,0,-126,12,0,124,0,-125,7,0,-124,12,0,124,0,-123,7,0,-122,12,0,124,0,-121,7,0,
            -120,12,0,124,0,-119,7,0,-118,12,0,124,0,-117,7,0,-116,12,0,124,0,-115,1,0,18,99,104,101,99,107,115,117,109,47,99,104,101,99,107,95,115,117,109,1,0,16,106,97,118,97,
            47,108,97,110,103,47,79,98,106,101,99,116,1,0,16,106,97,118,97,47,117,116,105,108,47,76,111,99,97,108,101,1,0,4,82,79,79,84,1,0,18,76,106,97,118,97,47,117,116,105,
            108,47,76,111,99,97,108,101,59,1,0,11,116,111,76,111,119,101,114,67,97,115,101,1,0,38,40,76,106,97,118,97,47,117,116,105,108,47,76,111,99,97,108,101,59,41,76,106,97,118,
            97,47,108,97,110,103,47,83,116,114,105,110,103,59,1,0,8,99,111,110,116,97,105,110,115,1,0,27,40,76,106,97,118,97,47,108,97,110,103,47,67,104,97,114,83,101,113,117,101,110,
            99,101,59,41,90,1,0,30,99,111,109,47,97,108,105,98,97,98,97,47,102,97,115,116,106,115,111,110,47,74,83,79,78,65,114,114,97,121,1,0,6,116,111,74,83,79,78,1,0,38,
            40,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,41,76,106,97,118,97,47,108,97,110,103,47,79,98,106,101,99,116,59,1,0,8,116,111,83,116,114,105,110,103,1,
            0,20,40,41,76,106,97,118,97,47,108,97,110,103,47,83,116,114,105,110,103,59,1,0,8,104,97,115,104,67,111,100,101,1,0,3,40,41,73,1,0,17,106,97,118,97,47,108,97,110,
            103,47,73,110,116,101,103,101,114,1,0,4,40,73,41,73,1,0,14,106,97,118,97,47,108,97,110,103,47,66,121,116,101,1,0,4,40,66,41,73,1,0,15,106,97,118,97,47,108,97,
            110,103,47,83,104,111,114,116,1,0,4,40,83,41,73,1,0,14,106,97,118,97,47,108,97,110,103,47,76,111,110,103,1,0,4,40,74,41,73,1,0,15,106,97,118,97,47,108,97,110,
            103,47,70,108,111,97,116,1,0,4,40,70,41,73,1,0,16,106,97,118,97,47,108,97,110,103,47,68,111,117,98,108,101,1,0,4,40,68,41,73,1,0,19,106,97,118,97,47,108,97,
            110,103,47,67,104,97,114,97,99,116,101,114,1,0,4,40,67,41,73,1,0,17,106,97,118,97,47,108,97,110,103,47,66,111,111,108,101,97,110,1,0,4,40,90,41,73,0,33,0,26,
            0,27,0,0,0,0,0,10,0,1,0,28,0,29,0,1,0,30,0,0,0,51,0,1,0,1,0,0,0,5,42,-73,0,1,-79,0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,
            6,0,4,0,7,0,32,0,0,0,12,0,1,0,0,0,5,0,33,0,34,0,0,0,9,0,35,0,36,0,1,0,30,0,0,1,49,0,4,0,7,0,0,0,-126,43,-63,0,2,-103,
            0,93,16,7,-67,0,2,89,3,18,3,83,89,4,18,4,83,89,5,18,5,83,89,6,18,6,83,89,7,18,7,83,89,8,18,8,83,89,16,6,18,9,83,77,44,78,45,-66,54,4,
            3,54,5,21,5,21,4,-94,0,35,45,21,5,50,58,6,43,-64,0,2,-78,0,10,-74,0,11,25,6,-74,0,12,-103,0,5,3,-84,-124,5,1,-89,-1,-36,43,-63,0,13,-102,0,10,43,
            -63,0,14,-103,0,5,3,-84,43,-72,0,15,-74,0,16,-74,0,17,-72,0,18,-84,77,3,-84,0,1,0,113,0,126,0,127,0,14,0,3,0,31,0,0,0,46,0,11,0,0,0,10,0,
            7,0,11,0,49,0,12,0,71,0,13,0,89,0,14,0,91,0,12,0,97,0,18,0,111,0,19,0,113,0,22,0,127,0,23,0,-128,0,24,0,32,0,0,0,52,0,5,0,71,0,
            20,0,37,0,38,0,6,0,49,0,48,0,39,0,40,0,2,0,-128,0,2,0,41,0,42,0,2,0,0,0,-126,0,43,0,44,0,0,0,0,0,-126,0,45,0,46,0,1,0,47,0,
            0,0,39,0,6,-1,0,58,0,6,1,7,0,48,7,0,49,7,0,49,1,1,0,0,32,-1,0,5,0,2,1,7,0,48,0,0,13,1,77,7,0,50,0,9,0,35,0,51,0,1,
            0,30,0,0,0,68,0,2,0,2,0,0,0,12,26,27,-72,0,19,96,59,26,-72,0,18,-84,0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,29,0,7,0,30,0,32,0,0,
            0,22,0,2,0,0,0,12,0,43,0,44,0,0,0,0,0,12,0,52,0,53,0,1,0,9,0,35,0,54,0,1,0,30,0,0,0,68,0,2,0,2,0,0,0,12,26,27,-72,0,
            20,96,59,26,-72,0,18,-84,0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,34,0,7,0,35,0,32,0,0,0,22,0,2,0,0,0,12,0,43,0,44,0,0,0,0,0,12,
            0,55,0,56,0,1,0,9,0,35,0,57,0,1,0,30,0,0,0,68,0,2,0,2,0,0,0,12,26,27,-72,0,18,96,59,26,-72,0,18,-84,0,0,0,2,0,31,0,0,0,10,
            0,2,0,0,0,39,0,7,0,40,0,32,0,0,0,22,0,2,0,0,0,12,0,43,0,44,0,0,0,0,0,12,0,58,0,44,0,1,0,9,0,35,0,59,0,1,0,30,0,0,
            0,68,0,3,0,3,0,0,0,12,26,31,-72,0,21,96,59,26,-72,0,18,-84,0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,44,0,7,0,45,0,32,0,0,0,22,0,2,
            0,0,0,12,0,43,0,44,0,0,0,0,0,12,0,60,0,61,0,1,0,9,0,35,0,62,0,1,0,30,0,0,0,68,0,2,0,2,0,0,0,12,26,35,-72,0,22,96,59,26,
            -72,0,18,-84,0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,49,0,7,0,50,0,32,0,0,0,22,0,2,0,0,0,12,0,43,0,44,0,0,0,0,0,12,0,63,0,64,
            0,1,0,9,0,35,0,65,0,1,0,30,0,0,0,68,0,3,0,3,0,0,0,12,26,39,-72,0,23,96,59,26,-72,0,18,-84,0,0,0,2,0,31,0,0,0,10,0,2,0,0,
            0,54,0,7,0,55,0,32,0,0,0,22,0,2,0,0,0,12,0,43,0,44,0,0,0,0,0,12,0,66,0,67,0,1,0,9,0,35,0,68,0,1,0,30,0,0,0,68,0,2,
            0,2,0,0,0,12,26,27,-72,0,24,96,59,26,-72,0,18,-84,0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,59,0,7,0,60,0,32,0,0,0,22,0,2,0,0,0,12,
            0,43,0,44,0,0,0,0,0,12,0,69,0,70,0,1,0,9,0,35,0,71,0,1,0,30,0,0,0,68,0,2,0,2,0,0,0,12,26,27,-72,0,25,96,59,26,-72,0,18,-84,
            0,0,0,2,0,31,0,0,0,10,0,2,0,0,0,64,0,7,0,65,0,32,0,0,0,22,0,2,0,0,0,12,0,43,0,44,0,0,0,0,0,12,0,52,0,72,0,1,0,1,
            0,73,0,0,0,2,0,74
    };
    private static List<String> skipClass = new ArrayList<>();
    @Test
    public void test() throws IOException {
        int i= 0;
        for (byte b : readFromByteFile("D:\\我的文件\\java程序\\interClassTest\\out\\production\\interClassTest\\checksum\\check_sum.class")) {
            System.out.print(b+",");
            i++;
            if(i == 50){
                i=0;
                System.out.println();
            }
        }
    }

    public static void checksumForProject(boolean checksum, List<ClassInfo> seeds, String mutationHistoryPath, ProjectInfo targetProject){
        if(!checksum){
            return;
        }
        List<ClassInfo> tempSeeds = new ArrayList<>();
        for(ClassInfo seed :seeds){
            String classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
            MainHelper.createFolderIfNotExist(classFileFolder);
            SootClass seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            try {
                ChecksumHelper.checksumForClass(seedClass);
            }catch (Exception e){
                e.printStackTrace();
                tempSeeds.add(seed);
                Scene.v().removeClass(seedClass);
                continue;
            }

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
            if(JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)){
                tempSeeds.add(newMutateClass);
            }else {
                tempSeeds.add(seed);
            }

            for (SootMethod method : seedClass.getMethods()) {
                method.releaseActiveBody();
            }
            Scene.v().removeClass(seedClass);

        }
        seeds.clear();
        seeds.addAll(tempSeeds);
    }
    /**
     * 设置哪些类是不进行checksum的
     * @param Path
     */
    public static void setSkipClass(String Path) throws IOException {
        File file = new File(Path);
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            return;
        }
        Set<String> set = new HashSet<>();
        String line = bufferedReader.readLine();
        while (line != null){
            System.out.println(line);
            set.add(line);
            line = bufferedReader.readLine();
        }
        skipClass.addAll(set);
    }

    private static boolean isSkipClass(String name){
        if(skipClass.isEmpty()){
            return false;
        }
        for (String aClass : skipClass) {
            if(aClass.equals(name)){
                return true;
            }
        }
        return false;
    }

    /**
     * 创建 check_sum.class 文件
     * @param pathname
     * @throws IOException
     */
    public static void createChecksumFile(String pathname) throws IOException {
        File pac = new File(pathname+ DTPlatform.FILE_SEPARATOR+"checksum");
        if (!pac.exists()) {
            pac.mkdirs();
        }
        File filename = new File(pathname+ DTPlatform.FILE_SEPARATOR+"checksum"+ DTPlatform.FILE_SEPARATOR+"check_sum.class");
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
        out.write(bytes);
        out.close();
    }

    /**
     * 对一个 sootclass 添加 checksum 相关指令
     * @param sootClass
     * @return
     */
    public static void checksumForClass(SootClass sootClass){
        if(isSkipClass(sootClass.getName())){
            return;
        }
        boolean hasClinit = false;
        // 判断是否有 clinit 函数
        for (SootMethod method : sootClass.getMethods()) {
            if(method.toString().contains("clinit")){
                hasClinit = true;
            }
        }
        // 添加 clinit 函数
        if (!hasClinit){
            SootMethod method = new SootMethod("<clinit>",new ArrayList<>(), VoidType.v(),8);
            JimpleBody body = Jimple.v().newBody(method);
            body.getUnits().add(new JReturnVoidStmt());
            sootClass.addMethod(method);
            method.setActiveBody(body);
        }
        // 插入全局变量
        SootMethod method = sootClass.getMethod("void <clinit>()");
        insertGlobalVarStmt(sootClass, method.retrieveActiveBody());
        for (SootMethod sootClassMethod : sootClass.getMethods()) {
            if(sootClassMethod.toString().contains("<init>") || sootClassMethod.toString().contains("<clinit>")){
                continue;
            }
            Body body =null;
            try{
                body = sootClassMethod.retrieveActiveBody();
            }catch (Exception e){
                continue;
            }
            List<Local> locals = new ArrayList<>(body.getLocals());
            // 插入checksum语句
            for (Local local : locals) {
                insertCheckSumStmtAfterLastWrite(sootClass,sootClassMethod.retrieveActiveBody(),local,getLastWriteMap(body));
            }
            // 插入输出语句
            if(sootClassMethod.toString().contains("main")){
                insertPrintStmtBeforeReturn(sootClass,body);
            }
        }
    }

    /**
     * 对新添加的 local 变量进行 checksum
     * @param sootClass 类
     * @param body 函数体
     * @param locals 新 local
     */
    public static void updateCheckSumStmtAfterLastWrite(SootClass sootClass,Body body,List<Local> locals){
        if(isSkipClass(sootClass.getName())){
            return;
        }
        Map<String,Unit> map = getLastWriteMap(body);
        for (Local local : locals) {
            Unit targetUnit = map.get(local.getName());
            if(targetUnit instanceof JIdentityStmt){
                return;
            }
            List<Stmt> stmts = getCheckSumStmt(sootClass,body,local);
            if(stmts != null){
                if(targetUnit != null) {
                    body.getUnits().insertAfter(stmts, targetUnit);
                }
            }

        }
    }

    /**
     * 读字节码文件用的
     * @param pathname
     * @return
     * @throws IOException
     */
    private static byte[] readFromByteFile(String pathname) throws IOException {
        File filename = new File(pathname);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] temp = new byte[1024];
        int size = 0;
        while((size = in.read(temp)) != -1){
            out.write(temp, 0, size);
        }
        in.close();
        byte[] content = out.toByteArray();
        return content;
    }

    /**
     * 获得所有 local 变量 最后一次被 write 时的语句映射
     * @param body
     * @return
     */
    private static Map<String,Unit> getLastWriteMap(Body body){
        // 查找每一个Local最后被write的位置
        Map<String,Unit> unUseSet = new HashMap<>(); // 存储还未被使用的局部变量
        Map<String,Unit> lwMap = new HashMap<>(); // LastWriteMap
        // 只分析赋值语句和调用语句
        for (Unit unit : body.getUnits()) {
            if (unit instanceof JAssignStmt || unit instanceof JInvokeStmt) {
                // 获取语句中的local
                for (ValueBox useAndDefBox : unit.getUseAndDefBoxes()) {
                    if (useAndDefBox.getValue() instanceof Local) {

                        String localName = ((Local) useAndDefBox.getValue()).getName();
//                        lwMap.put(localName,unit);
                        // 二次以上使用使用直接覆盖
                        if (lwMap.containsKey(localName)) {
                            lwMap.put(localName, unit);
                            continue;
                        }
                        // 第二次使用
                        if (unUseSet.containsKey(localName)) {
                            lwMap.put(localName, unit);
                            unUseSet.remove(localName);
                            continue;
                        }
                        // 第一次使用
                        unUseSet.put(localName, unit);
                    }
                }
            }
        }
        return lwMap;
    }
    /**
     * 在对于 Value 的最后一次赋值时，插入checksum
     * @param sootClass
     * @param body
     * @param value
     */
    private static void insertCheckSumStmtAfterLastWrite(SootClass sootClass,Body body,Value value,Map<String,Unit> map){

        Unit targetUnit = map.get(value.toString());
        if(targetUnit instanceof JIdentityStmt){
            return;
        }
        List<Stmt> stmts = getCheckSumStmt(sootClass,body,value);
        if(stmts!=null){
            if(targetUnit != null) {
                body.getUnits().insertAfter(stmts, targetUnit);
            }
        }

    }

    /**
     * 在 Return 语句之前 插入输出语句
     * @param sootClass
     * @param body
     */
    private static void insertPrintStmtBeforeReturn(SootClass sootClass,Body body){
        List<Unit> units = new ArrayList<>(body.getUnits());
        for (Unit unit : units) {
            if(unit instanceof JReturnVoidStmt || unit instanceof JReturnStmt){
                boolean flag = true;
                for (Stmt stmt : getPrintStmt(sootClass, body)) {
                    if(flag){
                        unit.redirectJumpsToThisTo(stmt);
                        flag = false;
                    }
                    body.getUnits().insertBeforeNoRedirect(stmt,unit);
                }
            }
        }
    }

    /**
     * 插入全局变量的语句序列
     * @param sootClass 要进行插入的类
     * @return
     */
    private static List<Stmt> insertGlobalVarStmt(SootClass sootClass, Body body){
        List<Stmt> stmtList = new ArrayList<>();
        // 添加全局变量my_check_sum
        SootField sootField= new SootField("my_check_sum",IntType.v(),9);
        sootClass.addField(sootField);
        // 将my_check_sum赋值为 0
        Value left = Jimple.v().newStaticFieldRef(sootField.makeRef());
        Value right = IntConstant.v(0);
        JAssignStmt assignStmt1 = new JAssignStmt(left,right);
        stmtList.add(assignStmt1);
        // 将my_check_sum赋值给var1
        left = new JimpleLocal(getVarName("var1", body.getLocals()),IntType.v());
        Value var1 = left;
        right = Jimple.v().newStaticFieldRef(sootField.makeRef());
        JAssignStmt assignStmt2 = new JAssignStmt(left,right);
        stmtList.add(assignStmt2);
        // 将my_check_sum赋值给var2
        left = new JimpleLocal(getVarName("var2", body.getLocals()),IntType.v());
        Value var2 = left;
        right = Jimple.v().newStaticFieldRef(sootField.makeRef());
        JAssignStmt assignStmt3 = new JAssignStmt(left,right);
        stmtList.add(assignStmt3);
        //调用check_sum:int run(int,int)
        left = new JimpleLocal(getVarName("var3", body.getLocals()),IntType.v());
        Value var3 = left;
        List<Value> args = new ArrayList<>();
        args.add(var1);
        args.add(var2);
        SootClass check_sum_class = JMUtils.loadTargetClass("checksum.check_sum");
        right = new JStaticInvokeExpr(check_sum_class.getMethod("int run(int,int)").makeRef(),args);
        for (SootMethod method : check_sum_class.getMethods()) {
            method.releaseActiveBody();
        }
        Scene.v().removeClass(check_sum_class);
        JAssignStmt assignStmt4 = new JAssignStmt(left,right);
        stmtList.add(assignStmt4);
        // 将var3赋值给my_check_sum
        left = Jimple.v().newStaticFieldRef(sootField.makeRef());
        right = var3;
        JAssignStmt assignStmt5 = new JAssignStmt(left,right);
        stmtList.add(assignStmt5);
        body.getLocals().add((Local) var1);
        body.getLocals().add((Local) var2);
        body.getLocals().add((Local) var3);
        List<Unit> units = new ArrayList<>(body.getUnits());
        for (Stmt stmt : stmtList) {
            body.getUnits().insertBeforeNoRedirect(stmt,body.getUnits().getLast());
        }
        return stmtList;
    }

    /**
     * 输出 check_sum的 语句序列
     * @param sootClass 要进行插入的类
     * @return
     */
    private static List<Stmt> getPrintStmt(SootClass sootClass, Body body){

        List<Stmt> stmtList = new ArrayList<>();
        // 新建 out
        SootClass systemClass = JMUtils.loadTargetClass("java.lang.System");
        SootField sootField = systemClass.getField("java.io.PrintStream out");
        Value left = new JimpleLocal(getVarName("var1", body.getLocals()),sootField.getType());
        Value var1 = left;
        Value right = Jimple.v().newStaticFieldRef(sootField.makeRef());
        JAssignStmt assignStmt1 = new JAssignStmt(left,right);
        stmtList.add(assignStmt1);
        // 输出 my_check_sum_value:
        SootClass streamClass = Scene.v().forceResolve("java.io.PrintStream",SootClass.BODIES);
        Scene.v().loadNecessaryClasses();
        List<Value> args = new ArrayList<>();
        args.add(StringConstant.v("my_check_sum_value:"));
        JInvokeStmt jInvokeStmt1 = new JInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) var1,streamClass.getMethod("void print(java.lang.String)").makeRef(),args));
        stmtList.add(jInvokeStmt1);
        // 新建out
        sootField = systemClass.getField("java.io.PrintStream out");
        left = new JimpleLocal(getVarName("var2", body.getLocals()),sootField.getType());
        Value var2 = left;
        right = Jimple.v().newStaticFieldRef(sootField.makeRef());
        JAssignStmt assignStmt2 = new JAssignStmt(left,right);
        stmtList.add(assignStmt2);
        // 取 my_check_sum
        left = new JimpleLocal(getVarName("var3", body.getLocals()),IntType.v());
        Value var3 = left;
        right = Jimple.v().newStaticFieldRef(sootClass.getField("my_check_sum",IntType.v()).makeRef());
        JAssignStmt assignStmt3 = new JAssignStmt(left,right);
        stmtList.add(assignStmt3);
        // 输出
        args.clear();
        args.add(var3);
        JInvokeStmt jInvokeStmt2 = new JInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) var2,streamClass.getMethod("void println(int)").makeRef(),args));
        stmtList.add(jInvokeStmt2);
        // 添加 local
        body.getLocals().add((Local) var1);
        body.getLocals().add((Local) var2);
        body.getLocals().add((Local) var3);


        for (SootMethod method : streamClass.getMethods()) {
            method.releaseActiveBody();
        }
        for (SootMethod method : systemClass.getMethods()) {
            method.releaseActiveBody();
        }
        Scene.v().removeClass(systemClass);
        Scene.v().removeClass(streamClass);
        return stmtList;
    }



    /**
     * 对变量 value 计算 check_sum 的语句
     * @param sootClass 要进行插入的类
     * @param value 要进行计算的 value
     * @return
     */
    private static List<Stmt> getCheckSumStmt(SootClass sootClass, Body body, Value value){
        List<Stmt> stmtList = new ArrayList<>();
        // 取 my_check_sum
        Value left = new JimpleLocal(getVarName("var1", body.getLocals()),IntType.v());
        Value var1 = left;
        Value right = Jimple.v().newStaticFieldRef(sootClass.getField("my_check_sum",IntType.v()).makeRef());
        JAssignStmt assignStmt1 = new JAssignStmt(left,right);
        stmtList.add(assignStmt1);
        // 调用run函数
        String type = "";
        switch (value.getType().toString()){
            case "byte":type ="byte";break;
            case "short":type ="short";break;
            case "int":type ="int";break;
            case "long":type ="long";break;
            case "float":type ="float";break;
            case "double":type ="double";break;
            case "char":type ="char";break;
            case "boolean":type ="boolean";break;
            default:return null;
//            default:type ="java.lang.Object";
        }
        left = new JimpleLocal(getVarName("var2", body.getLocals()),IntType.v());
        Value var2 = left;
        SootClass check_sum_class = JMUtils.loadTargetClass("checksum.check_sum");
        List<Value> args = new ArrayList<>();
        args.add(var1);
        args.add(value);
        right = new JStaticInvokeExpr(check_sum_class.getMethod("int run(int,"+type+")").makeRef(),args);
        JAssignStmt assignStmt2 = new JAssignStmt(left,right);
        stmtList.add(assignStmt2);
        // 覆盖 my_check_sum
        left = Jimple.v().newStaticFieldRef(sootClass.getField("my_check_sum",IntType.v()).makeRef());
        right = var2;
        JAssignStmt assignStmt3 = new JAssignStmt(left,right);
        stmtList.add(assignStmt3);
        body.getLocals().add((Local) var1);
        body.getLocals().add((Local) var2);
        for (SootMethod method : check_sum_class.getMethods()) {
            method.releaseActiveBody();
        }
        Scene.v().removeClass(check_sum_class);
        return stmtList;
    }

    /**
     * 得到一个独一无二的变量名
     * @param targetName 基础目标名
     * @param locals
     * @return targetName+"_N"*n
     */
    private static String getVarName(String targetName, Chain<Local> locals){
        for (Local local : locals) {
            if(local.getName().contains(targetName)){
                targetName=targetName+"_N";
            }
        }
        return targetName;
    }
}
