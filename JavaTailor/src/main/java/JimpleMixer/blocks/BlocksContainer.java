package JimpleMixer.blocks;

import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JThrowStmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.options.Options;
import soot.toolkits.graph.*;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
public class BlocksContainer {

    public static boolean containsGoto = false;
    public static Map<String, List<BlockInfo>> allMutationMap = new HashMap<>();
    public static Map<String, List<BlockInfo>> validMutationMap = new HashMap<>();
    public static List<BlockInfo> allMutations = new ArrayList<>();
    public static List<BlockInfo> validMutations = new ArrayList<>();
    public static List<List<BlockInfo>> allClusteringMutation = null;

    static {

        if (allMutationMap == null){
            allMutationMap = new HashMap<>();
        }
        if (validMutationMap == null){
            validMutationMap = new HashMap<>();
        }
        if (allMutations == null){
            allMutations = new ArrayList<>();
        }
        if (validMutations == null){
            validMutations = new ArrayList<>();
        }
    }

    /**
     * Class files are analyzed into five basic blocks
     * @param mutants
     */
    public static void initMutantsFromClasses(List<String> mutants) throws IOException {

        for (String mutation : mutants) {
            SootClass mutantClass = JMUtils.loadTargetClass(mutation);
            List<BlockInfo> blockMutants = BlocksContainer.getBlocksFromSootClass(mutantClass);
            allMutationMap.put(mutation, blockMutants);
            validMutationMap.put(mutation, filterInvalidBlocks(blockMutants));
        }

    }


    public static List<BlockInfo> getAllSuccessorBlockInBlocks(List<BlockInfo> explored, List<BlockInfo> blocks, BlockInfo currentBlock){

        List<BlockInfo> successors = new ArrayList<>();
        if (!explored.contains(currentBlock)){
            explored.add(currentBlock);
        }
        if (currentBlock instanceof StmtBlockInfo){
            successors.add(currentBlock);
        } else {
            successors.add(currentBlock);
            List<BlockInfo> succsBlocks = MutationHelper.getSuccesBlocks(blocks, currentBlock);
            for (BlockInfo succsBlock : succsBlocks) {
                if (!explored.contains(succsBlock)){
                    successors.addAll(getAllSuccessorBlockInBlocks(explored, blocks, succsBlock));
                }
            }

        }
        return successors;
    }

    public static List<Unit> getAllStmtsInBlocks(List<BlockInfo> blocks){

        List<Unit> allStmts = new ArrayList<>();
        for (BlockInfo block : blocks) {
            allStmts.addAll(block.getAllStmts());
        }
        return allStmts;
    }




    /**
     * A SOOT class was converted into five kinds of basic blocks
     * @param sootClass
     * @return
     */
    public static List<BlockInfo> getBlocksFromSootClass(SootClass sootClass){

        List<BlockInfo> allBlocks = new ArrayList<>();
        List<SootMethod> methodList = sootClass.getMethods();
        Chain<SootField> fieldList = sootClass.getFields();
        for (SootMethod method : methodList) {

            if (!method.isAbstract()){

                Body methodBody = method.retrieveActiveBody();
                if (!method.isConstructor()){

                    // 01 stmt-block, if-block , switch-block, return-block, TrapBlock
                    for (Block block : new ZonedBlockGraph(methodBody).getBlocks()) {

                        BlockInfo currentBlock = BlockAnalyzer.initialBlock(sootClass, block, methodBody);
                        if (currentBlock != null){
                            allBlocks.add(currentBlock);
                        }
                    }
                    //02 for-block
                    for (Loop loop : new LoopNestTree(methodBody)) {

                        BlockInfo loopBlock = BlockAnalyzer.initialLoopBlock(methodBody, allBlocks, loop);
                        if (loopBlock != null){
                            allBlocks.add(loopBlock);
                        }
                    }
                    //03 trap-block
                    for (Trap trap : methodBody.getTraps()) {

                        BlockInfo trapBlock = BlockAnalyzer.initialTrapBlock(methodBody, allBlocks, trap);
                    }
                }
            }
        }
        return allBlocks;
    }

    //???这个过滤过滤的都是什么
    /**
     * Filter out Invalid base blocks
     * @param allBlocks
     * @return
     */
    public static List<BlockInfo> filterInvalidBlocks(List<BlockInfo> allBlocks){

        List<BlockInfo> validBlocks =
                allBlocks.stream().filter(blockInfo -> ! (blockInfo instanceof ReturnStmtBlockInfo) &&
                        !(blockInfo.getValidStmts().size() == 1 && blockInfo.getValidStmts().get(0) instanceof JThrowStmt)
                ).collect(Collectors.toList());

        /**
         * add one goto block
         */
        if (!containsGoto){
            List<BlockInfo> gotos = validBlocks.stream().filter(blockInfo -> blockInfo.getValidStmts().size() == 0
                    && blockInfo instanceof StmtBlockInfo
                    && ((StmtBlockInfo)blockInfo).getGotoStmt() != null).collect(Collectors.toList());
            if (gotos.size() > 0){

                validMutations.add(gotos.get(0));
                containsGoto = true;
            }
        }
        validMutations.addAll(validBlocks.stream().filter(blockInfo -> (blockInfo instanceof LoopStmtBlockInfo || blockInfo.getValidStmts().size() > 0)).collect(Collectors.toList()));
        return validBlocks;
    }

    public static Map<String, List<BlockInfo>> getAllMutationMap() {
        return allMutationMap;
    }

    public static Map<String, List<BlockInfo>> getValidMutationMap() {
        return validMutationMap;
    }

    public static List<BlockInfo> getAllMutations() {
        return allMutations;
    }

    public static List<BlockInfo> getValidMutations() {
        return validMutations;
    }

    public static boolean isContainsGoto() {
        return containsGoto;
    }

    public static void setContainsGoto(boolean containsGoto) {
        BlocksContainer.containsGoto = containsGoto;
    }

    public static void setAllMutationMap(Map<String, List<BlockInfo>> allMutationMap) {
        BlocksContainer.allMutationMap = allMutationMap;
    }

    public static void setValidMutationMap(Map<String, List<BlockInfo>> validMutationMap) {
        BlocksContainer.validMutationMap = validMutationMap;
    }

    public static void setAllMutations(List<BlockInfo> allMutations) {
        BlocksContainer.allMutations = allMutations;
    }

    public static void setValidMutations(List<BlockInfo> validMutations) {
        BlocksContainer.validMutations = validMutations;
    }

    public static List<List<BlockInfo>> getAllClusteringMutation() {
        return allClusteringMutation;
    }

    public static void setAllClusteringMutation(List<List<BlockInfo>> allClusteringMutation) {
        BlocksContainer.allClusteringMutation = allClusteringMutation;
    }
}
