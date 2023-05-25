package dtjvms.analyzer;

import java.util.*;

/**
 * @Auther: Gao Tianchang
 * @Date: 2021/11/24/8:23
 * @Description: 通过这个类，你可以对英文字符串进行简单的相似度计算
 */
public class CosSimilarty {
    /**
     * 这是一个分隔符列表，在这里面包括了对英语进行分词的分隔符
     */
    private static final List<String> separators = new ArrayList<>();
    static {
        separators.add(" ");
        separators.add("\"");
        separators.add(":");
        separators.add("=");
        separators.add("\\(");
        separators.add("\\)");
        separators.add(";");
        separators.add("\n");
        separators.add("\t");
        separators.add("@");
        separators.add("\\{");
        separators.add("}");
        separators.add(">");
        separators.add("#");
    }

    /**
     * 方法唯一的可执行接口，通过这个接口可以获得两个英文字符串的余弦相似度
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 余弦相似度
     */
    public static double run(String str1,String str2){
        if (str1.equals(str2)) {
            return 1;
        }
        if(str1.equals("") || str2.equals("")){
            return 0;
        }
        List<String> str1List = eliminateSeparator(str1);
        List<String> str2List = eliminateSeparator(str2);
        Map<String,Integer> str1Map = calculateFrequency(str1List);
        Map<String,Integer> str2Map = calculateFrequency(str2List);
        return calculateCosSimilarty(str1Map,str2Map);
    }

    /**
     * 简易的分词函数，可以自行修改，只要保证返回的是String列表就可以
     * @param str 需要分词的字符串
     * @return 分割完的字符串
     */
    private static List<String> eliminateSeparator(String str){
        List<String> tempList = new ArrayList<>();
        tempList.add(str);
        for(String separator:separators){
            int size = tempList.size();
            for(int i=size;i>0;i--){
                String tempString = tempList.get(0);
                tempList.remove(0);
                tempList.addAll(Arrays.asList(tempString.split(separator)));
            }
        }
        for(int i = 0;i<tempList.size();i++){
            if(tempList.get(i).length()==0){
                tempList.remove(i);
                i--;
            }
//            else if(tempList.get(i).contains(".")){
//                tempList.remove(i);
//                i--;
//            }else if(tempList.get(i).contains("/")){
//                tempList.remove(i);
//                i--;
//            }
        }
        return tempList;
    }

    /**
     * 计算分词后的字符串的频率
     * @param strList 字符串列表
     * @return 字符串-频率的键值对
     */
    private static Map<String,Integer> calculateFrequency(List<String> strList){
        Map<String,Integer> tempMap = new HashMap<>();
        for(String str:strList){
            Integer integer = tempMap.get(str);
            if(integer == null){
                integer = 1;
            }else {
                integer = integer + 1;
            }
            tempMap.put(str,integer);
        }
        return tempMap;
    }

    /**
     * 计算余弦相似度
     * @param map1 字符串1的键值对
     * @param map2 字符串2的键值对
     * @return 余弦相似度
     */
    private static double calculateCosSimilarty(Map<String,Integer> map1,Map<String,Integer> map2){
        Set<String> set =new HashSet<>();
        set.addAll(map1.keySet());
        set.addAll(map2.keySet());
        for(String s:set){
            map1.putIfAbsent(s, 0);
            map2.putIfAbsent(s, 0);
        }
        double xy = 0;
        double x = 0;
        double y = 0;
        for(String s:set){
            Integer integer1 = map1.get(s);
            Integer integer2 = map2.get(s);
            xy += integer1*integer2;
            x += integer1*integer1;
            y += integer2*integer2;
        }
        x = Math.sqrt(x);
        y = Math.sqrt(y);
        return xy/(x*y);
    }

}
