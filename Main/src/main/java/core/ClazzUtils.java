package core; /**
 * @Auther: Gao Tianchang
 * @Date: 2021/11/02/7:58
 * @Description:
 */
import dtjvms.DTPlatform;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.lang.ClassLoader;
public class ClazzUtils {
    public static String projectPath = "";
    public static String packagePath = "";
    public static String packageName = "";
    public static void main(String[] args) throws Exception {
        projectPath = "02Benchmarks/commons-math-master";
        packagePath = "target/classes";
        packageName = "org";
        List<String> classList = run(projectPath,packagePath,packageName);
        for(String s:classList){
            System.out.println(s);
        }
    }

    /**
     * 递归获取指定路径下的全部类
     * @param pathName
     * @param packageName
     * @return
     */
    public static List<String> run(String projectPath,String packagePath,String packageName){
        ClazzUtils.projectPath = projectPath;
        ClazzUtils.packagePath = packagePath;
        ClazzUtils.packageName = packageName;
        File root;
        if(Objects.equals(packagePath, "")){
            root = new File(projectPath+DTPlatform.FILE_SEPARATOR+packageName);
        }else {
            root = new File(projectPath+DTPlatform.FILE_SEPARATOR+packagePath+DTPlatform.FILE_SEPARATOR+packageName);
        }
        return loop(root, packageName);
    }
    public static List<String> loop(File folder, String packageName){
        List<String> stringList = new ArrayList<>();
        File[] files = folder.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {

                stringList.addAll(loop(file, packageName+"."+file.getName()));


            } else {
                String temp = listMethodNames(file.getName(), packageName);
                if(!temp.equals("")){
                    stringList.add(temp);
                }
            }
        }
        return stringList;
    }
    public static String listMethodNames(String filename, String packageName) {
        try {
            if (filename.contains(".class")){
                String name = filename.substring(0, filename.length() - 6);
                File file=new File(projectPath+DTPlatform.FILE_SEPARATOR+packagePath);
                URL url=file.toURI().toURL();
                URL[] urls=new URL[]{url};
                ClassLoader cl=new URLClassLoader(urls);
                Class cls=cl.loadClass(packageName+"."+name);
                if(cls.isInterface()){
                    return "";
                }
                if(Modifier.isAbstract(cls.getModifiers())){
                    return "";
                }
                return  packageName + "."+name;
            }
        } catch (NoClassDefFoundError | MalformedURLException | ClassNotFoundException e) {
            return "";
        }
        return "";
    }
}
