package core;

import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import dtjvms.DTGlobal;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.junit.Test;

import java.util.*;

public class SelectBlockHelper {

    public static boolean reshape = true;
    private static int SELECT_TIME = 0;
    public static final int RANDOM_SELECT = 0;
    public static final int SOFTMAX_SELECT = 1;
    public static final int RWS_SELECT = 2;
    /**
     * Gao Tianchang：通过method字符串选择选择方法
     * @param method
     * @param random
     * @return
     */
    public static BlockInfo getIngredient(int method, Random random){
        SELECT_TIME++;
        if(method == RANDOM_SELECT){
            return getIngredientRandom(random);
        }
        if(method == SOFTMAX_SELECT){
            return getIngredientSoftmax(random);
        }
        if(method == RWS_SELECT){
            return getIngredientRWS(random);
        }

        return null;
    }
    /**
     * Obtain a random ingredient from the dictionary
     * @param random
     * @return
     */
    private static BlockInfo getIngredientRandom(Random random){
        if(reshape){
            List<List<BlockInfo>> AllClusteringMutation = BlocksContainer.getAllClusteringMutation();
            List<BlockInfo> blockInfos = new ArrayList<>();
            while (blockInfos.size()==0){
                blockInfos = AllClusteringMutation.get(random.nextInt(AllClusteringMutation.size()));
            }
            return blockInfos.get(random.nextInt(blockInfos.size()));
        }else {
            Map<String, List<BlockInfo>> ingredients = BlocksContainer.getValidMutationMap();
            Set<String> classCandidate = ingredients.keySet();
            String candidate = (String) classCandidate.toArray()[random.nextInt(classCandidate.size())];
            List<BlockInfo> methodCandidate = ingredients.get(candidate);

            if (methodCandidate.size() > 0){
                return methodCandidate.get(random.nextInt(methodCandidate.size()));
            }
            return null;
        }
    }

    /**
     * Gao Tianchang：使softmax方法进行选择ingredient
     * @param random
     * @return
     */
    private static BlockInfo getIngredientSoftmax(Random random){
        List<List<BlockInfo>> AllClusteringMutation = BlocksContainer.getAllClusteringMutation();
        List<List<Integer>> diffFoundTimeTable=new ArrayList<>();
        List<Integer> sumDiffFoundTimeTable = new ArrayList<>();
        for(List<BlockInfo> blockInfos:AllClusteringMutation){
            List<Integer> diffFoundTimeList = new ArrayList<>();
            int sum = 0;
            for(BlockInfo blockInfo:blockInfos){
                diffFoundTimeList.add(blockInfo.getDiffFoundTimes());
                sum+=blockInfo.getDiffFoundTimes();
            }
            diffFoundTimeTable.add(diffFoundTimeList);
            sumDiffFoundTimeTable.add(sum);
        }
        int classIndex = genSoftmaxIndex(sumDiffFoundTimeTable.toArray(new Integer[0]),random);
        int BlockIndex = genSoftmaxIndex(diffFoundTimeTable.get(classIndex).toArray(new Integer[0]),random);
        return AllClusteringMutation.get(classIndex).get(BlockIndex);
    }

    /**
     * Gao Tianchang：计算softmax概率并且选择一个index
     * @param diffFoundTimes
     * @param random
     * @return
     */
    private static int genSoftmaxIndex(Integer[] diffFoundTimes,Random random){
        double[] probability = new double[diffFoundTimes.length];
        double sum = 0;
        for(int i=0;i< diffFoundTimes.length;i++){
            sum+=Math.exp(diffFoundTimes[i]);
        }
        for(int i=0;i< diffFoundTimes.length;i++){
            probability[i] = Math.exp(diffFoundTimes[i])/sum;
        }
        double threshold = 0;
        while (threshold == 0){
            threshold = random.nextDouble();
        }
        sum = 0;
        for(int i=0;i< probability.length;i++){
            sum+=probability[i];
            if(sum >= threshold){
                return i;
            }
        }
        return probability.length-1;
    }

    /**
     * Gao Tianchang：使RWS（roulette wheel selection）方法进行选择ingredient
     * @param random
     * @return
     */
    private static BlockInfo getIngredientRWS(Random random){
        List<List<BlockInfo>> AllClusteringMutation = BlocksContainer.getAllClusteringMutation();
//        List<List<Double>> succeedTable=new ArrayList<>();
        List<Double> sumSucceedTable = new ArrayList<>();
        List<String> logInfoList = new ArrayList<>();
        for(List<BlockInfo> blockInfos:AllClusteringMutation){
//            List<Double> diffFoundTimeList = new ArrayList<>();
            int numerator = 0;
            int denominator = 0;
            double sum = 0;
            for(BlockInfo blockInfo:blockInfos){
//                int diffFoundTime = blockInfo.getDiffFoundTimes();
//                int choseTime = blockInfo.getChosenTimes();
//                if(diffFoundTime == 0){
//                    diffFoundTime = 1;
//                }
//                if(choseTime == 0){
//                    choseTime = 1;
//                }
//                diffFoundTimeList.add((double)diffFoundTime/(double)choseTime);
                numerator += blockInfo.getDiffFoundTimes();
                denominator += blockInfo.getChosenTimes();
            }
            logInfoList.add(""+(numerator)+","+(denominator));
            if(numerator == 0){
                numerator = 1;
            }
            if(denominator == 0){
                denominator = 1;
            }
            sum = (double) numerator/(double) denominator;
//            succeedTable.add(diffFoundTimeList);
            sumSucceedTable.add(sum);
        }

        int classIndex = 0;
        int BlockIndex = 0;
        do {
            classIndex = genRWSIndex(sumSucceedTable.toArray(new Double[0]),random,logInfoList);
        }while (AllClusteringMutation.get(classIndex).size() == 0);
        DTGlobal.getSelectLogger().info("第"+classIndex+"类被选择了");
        BlockIndex = random.nextInt(AllClusteringMutation.get(classIndex).size());
        return AllClusteringMutation.get(classIndex).get(BlockIndex);
    }
    private static int genRWSIndex(Double[] succeed,Random random,List<String> logInfoList){
        double[] probability = new double[succeed.length];
        double sum = 0;
        for (Double aDouble : succeed) {
            sum += aDouble;
        }
        for(int i=0;i< succeed.length;i++){
            probability[i] = succeed[i]/sum;
        }
        double threshold = 0;
        while (threshold == 0){
            threshold = random.nextDouble();
        }
        sum = 0;
        for(int i=0;i< probability.length;i++){
            sum+=probability[i];
            if(sum >= threshold){
                return i;
            }
        }
        return probability.length-1;
    }
    /**
     * Gao Tianchang：使MAB(Multi-armed Bandit)方法进行选择ingredient   Epsilon Greedy
     * @param random
     * @return
     */
    private static BlockInfo getIngredientMAB(Random random){
        List<List<BlockInfo>> AllClusteringMutation = BlocksContainer.getAllClusteringMutation();
        // 每一个block的成功率
        List<List<Double>> blockSucceed=getSucceed();
        // 每一类的成功率
//        List<Double> clusterSucceed = blockSucceed.get(blockSucceed.size() - 1);
        blockSucceed.remove(blockSucceed.size() - 1);
        List<BetaDistribution> clusterDistribution = getDistribute();
        int classIndex = genMABIndexThompson(clusterDistribution);
        clusterDistribution.clear();
//        int classIndex = genMABIndexGreedy(clusterSucceed.toArray(new Double[0]),random);
        DTGlobal.getSelectLogger().info("第"+classIndex+"类被选择了");
        int BlockIndex = random.nextInt(blockSucceed.get(classIndex).size());
        return AllClusteringMutation.get(classIndex).get(BlockIndex);
    }
    private static int genMABIndexGreedy(Double[] succeed, Random random){
        double epsilon = 0.5;
        // 探索
        if(random.nextDouble()<epsilon){
            return random.nextInt(succeed.length);
        }// 贪婪
        else {
            double max = -1;
            int index = -1;
            for(int i = 0;i < succeed.length;i++){
                if(succeed[i] > max){
                    max = succeed[i];
                    index = i;
                }
            }
            return index;
        }
    }
    private static int genMABIndexThompson(List<BetaDistribution> clusterDistribution){
        List<Double> sample = new ArrayList<>();
        for (BetaDistribution betaDistribution : clusterDistribution) {
            sample.add(betaDistribution.sample());
        }
        int index = -1;
        double max = -1;
        for(int i = 0;i<sample.size();i++){
            if(sample.get(i)>max){
                max  = sample.get(i);
                index = i;
            }
        }
        return index;
    }

    private static List<BetaDistribution> getDistribute(){
        List<List<BlockInfo>> AllClusteringMutation = BlocksContainer.getAllClusteringMutation();
        // 每一类的分布
        List<BetaDistribution> clusterDistribute = new ArrayList<>();
        for(List<BlockInfo> blockInfos:AllClusteringMutation){
            double numerator = 0;
            double denominator = 0;
            double sum = 0;
            for(BlockInfo blockInfo:blockInfos){
                numerator += blockInfo.getDiffFoundTimes();
                denominator += blockInfo.getChosenTimes();
            }
            double alpha = numerator + 1;
            double beta = denominator - numerator + 1;

            clusterDistribute.add(new BetaDistribution(alpha,beta));
        }
        return clusterDistribute;
    }
    private static List<List<Double>> getSucceed(){
        List<List<BlockInfo>> AllClusteringMutation = BlocksContainer.getAllClusteringMutation();
        // 每一个block的成功率
        List<List<Double>> blockSucceed=new ArrayList<>();
        // 每一类的成功率
        List<Double> clusterSucceed = new ArrayList<>();
        for(List<BlockInfo> blockInfos:AllClusteringMutation){
            List<Double> diffFoundTimeList = new ArrayList<>();
            int numerator = 0;
            int denominator = 0;
            double sum = 0;
            for(BlockInfo blockInfo:blockInfos){
                int diffFoundTime = blockInfo.getDiffFoundTimes();
                int choseTime = blockInfo.getChosenTimes();
                if(diffFoundTime == 0){
                    diffFoundTime = 1;
                }
                if(choseTime == 0){
                    choseTime = 1;
                }
                diffFoundTimeList.add((double)diffFoundTime/(double)choseTime);
                numerator += blockInfo.getDiffFoundTimes();
                denominator += blockInfo.getChosenTimes();
            }
            if(numerator == 0){
                numerator = 1;
            }
            if(denominator == 0){
                denominator = 1;
            }
            sum = (double) numerator/(double) denominator;
            blockSucceed.add(diffFoundTimeList);
            clusterSucceed.add(sum);
        }
        blockSucceed.add(clusterSucceed);
        return blockSucceed;
    }

    public static int getSelectTime() {
        return SELECT_TIME;
    }
}
