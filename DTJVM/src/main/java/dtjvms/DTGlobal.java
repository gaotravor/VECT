package dtjvms;

import util.LoggerUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.*;

public class DTGlobal {

    public static boolean useVMOptions = false;

	private static long startTime;
    private static Logger logger;
    private static Logger diffLogger;
    private static Logger diffUniqueLogger;
    private static Logger rewardLogger;
    private static Logger dataLogger;
	private static Logger timeLogger;
    private static Logger selectLogger;
    private static List<String> insertInfo = new ArrayList<>();
    private static Logger insertLogger;

    public static List<String> getInsertInfo() {
        return insertInfo;
    }

    public static void setLogger(String timeStamp){

        if (logger == null){
            logger = LoggerUtils.getInstance(timeStamp);
        }
    }
    public static long getStartTime() {
        return startTime;
    }

    public static void setStartTime(long startTime) {
        DTGlobal.startTime = startTime;
    }

    /**
     *Get a difference testing log file
     * @param timeStamp
     * @param filename
     */
    public static void setDiffLogger(String timeStamp, String filename){
        diffLogger = LoggerUtils.getInstance(timeStamp, filename, true);
    }
    public static void setSelectLogger(String timeStamp,String filename){
        selectLogger = LoggerUtils.getInstance(timeStamp,filename,true);
    }
    public static void setInsertLogger(String timeStamp,String filename){
        insertLogger = LoggerUtils.getInstance(timeStamp,filename,true);
    }


    public static void setDiffUniqueLogger(String timeStamp, String filename){

        if (diffUniqueLogger == null){
            diffUniqueLogger = LoggerUtils.getInstance(timeStamp, filename, true);
        }
    }

    public static void setDataLogger(String timeStamp, String filename){

        if (dataLogger == null){
            dataLogger = LoggerUtils.getInstance(timeStamp, filename, true);
        }
    }

    public static void setTimeLogger(String timeStamp, String filename){

        if (timeLogger == null){
            timeLogger = LoggerUtils.getInstance(timeStamp, filename, true);
        }
    }

    public static void setRewardLogger(String timeStamp, String filename) {
        if (rewardLogger == null){
            rewardLogger = LoggerUtils.getInstance(timeStamp, filename, true);
        }
    }

    public static Logger getLogger(){
        return logger;
    }

    public static Logger getDiffLogger() {
        return diffLogger;
    }
    public static Logger getSelectLogger(){return selectLogger;}

    public static Logger getDiffUniqueLogger() {
        return dataLogger;
    }

    public static Logger getDataLogger() {
        return diffUniqueLogger;
    }

    public static Logger getTimeLogger() {
        return timeLogger;
    }

    public static Logger getRewardLogger() {
        return rewardLogger;
    }

    public static Logger getInsertLogger() {
        return insertLogger;
    }

}
