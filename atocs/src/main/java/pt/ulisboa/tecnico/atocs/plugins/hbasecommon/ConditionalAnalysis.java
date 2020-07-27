package pt.ulisboa.tecnico.atocs.plugins.hbasecommon;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.atocs.core.ConditionalStmt;
import pt.ulisboa.tecnico.atocs.core.IfConditionalStmt;
import pt.ulisboa.tecnico.atocs.core.SwitchConditionalStmt;
import pt.ulisboa.tecnico.atocs.core.datastructures.ValueState;

import java.util.*;
import java.util.stream.Collectors;

public class ConditionalAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(ConditionalAnalysis.class);

    public static <T extends ValueState> List<ValueState> performAnalysis(ValueState baseValueState,
                                                                   List<T> searchValueStates) {
        List<ValueState> obtainedFields = new ArrayList<>();
        List<List<T>> allPathFields = new ArrayList<>();
        List<ValueState> noIntersectionFields = new ArrayList<>();
        Set<T> alreadyAnalysed = new HashSet<>();
        for (T searchValueState : searchValueStates) {
            if (alreadyAnalysed.contains(searchValueState)) break;
            // Obtain the base ValueState to be used (which will be compared to the searchValueStates)
            ValueState tBaseValueState = baseValueState;
            if (searchValueState.hasParamMethodInvocations())
                tBaseValueState = searchValueState.getLastParamMethodInvocations();

            if (tBaseValueState.getStack().intersectConditionalStmts(searchValueState.getStack())) {
                int compare = tBaseValueState.getStack().currentNumConditionalStmts()
                        - searchValueState.getStack().currentNumConditionalStmts();
                if (compare >= 0) {
                    List<T> pathField = new ArrayList<>();
                    pathField.add(searchValueState);
                    allPathFields.add(pathField);
                } else {
                    allPathFields.add(doExplorePaths(searchValueState, tBaseValueState, searchValueStates,
                            alreadyAnalysed));
                }
            } else {
                ConditionalStmt lastIntersect = tBaseValueState.getStack().getLastCondStmtsIntersect(
                        searchValueState.getStack().getCurrentConditionalStmts());
                ConditionalStmt baseValueDiffStmt;
                ConditionalStmt valueDiffStmt;
                if (lastIntersect != null) {
                    baseValueDiffStmt = tBaseValueState.getStack().getSingleCurrentConditionalStmtsAfter(lastIntersect);
                    valueDiffStmt = searchValueState.getStack().getSingleCurrentConditionalStmtsAfter(lastIntersect);
                } else {
                    baseValueDiffStmt = tBaseValueState.getStack().getCurrentConditionalStmts().isEmpty()
                            ? null : tBaseValueState.getStack().getCurrentConditionalStmts().get(0);
                    valueDiffStmt = searchValueState.getStack().getCurrentConditionalStmts().isEmpty()
                            ? null : searchValueState.getStack().getCurrentConditionalStmts().get(0);
                }
                if (baseValueDiffStmt != null && valueDiffStmt != null) {
                    List<ConditionalStmt> dependents = new ArrayList<>();
                    if (baseValueDiffStmt instanceof IfConditionalStmt)
                        dependents.add(((IfConditionalStmt)baseValueDiffStmt).getDependent());
                    else if (baseValueDiffStmt instanceof SwitchConditionalStmt)
                        dependents.addAll(((SwitchConditionalStmt)baseValueDiffStmt).getDependents());
                    else
                        logger.warn("Unknown ConditionalStmt.");
                    if (!dependents.contains(valueDiffStmt)) { // case of two unrelated conditional block
                        allPathFields.add(doExplorePaths(searchValueState, tBaseValueState, searchValueStates,
                                alreadyAnalysed));
//                        List<T> pathField = new ArrayList<>();
//                        pathField.add(null);
//                        allPathFields.add(pathField);
                    } else {
                        noIntersectionFields.add(searchValueState);
                    }
                }
            }
            alreadyAnalysed.add(searchValueState);
        }

        boolean allNullPaths = true;
        for (List<T> pathFields : allPathFields) {
            if (!pathFields.contains(null)) {
                allNullPaths = false;
                break;
            }
        }
        obtainedFields.addAll(searchValueStates.stream().filter(v -> v != null &&
                !noIntersectionFields.contains(v)).collect(Collectors.toList()));
        if (allNullPaths)
            return new ArrayList<>();
        return obtainedFields;
    }

    static <T extends ValueState> List<T> doExplorePaths(ValueState searchValueState, ValueState tBaseValueState,
                                                         List<T> searchValueStates, Set<T> alreadyAnalysed) {
        List<ConditionalStmt> conditionalStmtsAfter = null;
        if (tBaseValueState.getStack().intersectConditionalStmts(searchValueState.getStack()))
            conditionalStmtsAfter = searchValueState.getStack()
                    .getCurrentConditionalStmtsAfter(tBaseValueState.getStack().getCurrentConditionalStmts());
        else
            conditionalStmtsAfter = searchValueState.getStack().getCurrentConditionalStmts();

        if (conditionalStmtsAfter == null || conditionalStmtsAfter.isEmpty())
            return new ArrayList<>();

        ConditionalStmt nextCondStmt = conditionalStmtsAfter.get(0);
        return explorePaths(nextCondStmt, searchValueStates, alreadyAnalysed);
    }

    static <T extends ValueState> List<T> explorePaths(ConditionalStmt nextConditionalStmt, List<T> valueStates,
                                                       Set<T> alreadyAnalysed) {
        if (nextConditionalStmt instanceof IfConditionalStmt)
            return exploreIfPaths((IfConditionalStmt) nextConditionalStmt, valueStates, alreadyAnalysed);
        else if (nextConditionalStmt instanceof SwitchConditionalStmt)
            return exploreSwitchPaths((SwitchConditionalStmt) nextConditionalStmt, valueStates, alreadyAnalysed);
        else
            logger.warn("Unknown ConditionalStmt.");
        return new ArrayList<>();
    }

    static <T extends ValueState> List<T> exploreIfPaths(IfConditionalStmt nextConditionalStmt, List<T> valueStates,
                                                         Set<T> alreadyAnalysed) {
        List<T> obtainedFields = new ArrayList<>();
        if (nextConditionalStmt == null) return obtainedFields;
        ConditionalStmt dependent = nextConditionalStmt.getDependent();
        if (dependent == null) {
            obtainedFields.add(null);
            alreadyAnalysed.addAll(getNextPathValues(nextConditionalStmt, valueStates));
            return obtainedFields;
        } else {
            List<T> nextPathValues1 = getNextPathValues(nextConditionalStmt, valueStates);
            List<T> nextPathValues2 = getNextPathValues(dependent, valueStates);
            if (nextPathValues1.isEmpty()) {
                alreadyAnalysed.addAll(nextPathValues1);
                obtainedFields.add(null);
            } else {
                obtainedFields.addAll(exploreNextPath(nextPathValues1, nextConditionalStmt, valueStates,
                        alreadyAnalysed));
            }
            if (nextPathValues2.isEmpty()) {
                alreadyAnalysed.addAll(nextPathValues2);
                obtainedFields.add(null);
            } else {
                obtainedFields.addAll(exploreNextPath(nextPathValues2, dependent, valueStates, alreadyAnalysed));
            }
        }
        return obtainedFields;
    }

    static <T extends ValueState> List<T> exploreSwitchPaths(SwitchConditionalStmt nextConditionalStmt,
                                                             List<T> valueStates, Set<T> alreadyAnalysed) {
        List<T> obtainedFields = new ArrayList<>();
        if (nextConditionalStmt == null) return obtainedFields;
        List<SwitchConditionalStmt> dependents = nextConditionalStmt.getDependents();
        if (dependents.isEmpty()) {
            obtainedFields.add(null);
            alreadyAnalysed.addAll(getNextPathValues(nextConditionalStmt, valueStates));
            return obtainedFields;
        } else {
            List<Pair<List<T>, SwitchConditionalStmt>> nextPathPairList = new ArrayList<>();
            dependents.add(nextConditionalStmt);
            boolean hasEmptyCase = false;
            for (SwitchConditionalStmt condStmt : dependents) {
                List<T> nextPathValues = getNextPathValues(condStmt, valueStates);
                if (nextPathValues.isEmpty()) {
                    alreadyAnalysed.addAll(nextPathValues);
                    hasEmptyCase = true;
                }
                nextPathPairList.add(new ImmutablePair<>(nextPathValues, condStmt));
            }
            if (hasEmptyCase || !nextConditionalStmt.hasDefaultDependent()) {
                obtainedFields.add(null);
                for (List<T> already : nextPathPairList.stream().map(Pair::getLeft).collect(Collectors.toList()))
                    alreadyAnalysed.addAll(already);
            } else {
                for (Pair<List<T>, SwitchConditionalStmt> nextPathPair : nextPathPairList) {
                    obtainedFields.addAll(exploreNextPath(nextPathPair.getLeft(), nextPathPair.getRight(), valueStates,
                            alreadyAnalysed));
                }
            }
        }
        return obtainedFields;
    }

    static <T extends ValueState> List<T> exploreNextPath(List<T> nextPathValues, ConditionalStmt prevCondStmt,
                                                          List<T> valueStates, Set<T> alreadyAnalysed) {
        List<T> obtainedFields = new ArrayList<>();
        T nextPathExplorer = null;
        for (T nextPathValue : nextPathValues) {
            if (prevCondStmt.equals(nextPathValue.getStack().getCurrentLastConditionalStmt())) {
                alreadyAnalysed.add(nextPathValue);
                obtainedFields.add(nextPathValue);
            } else {
                nextPathExplorer = nextPathValue;
            }
        }
        if (nextPathExplorer != null) {
            obtainedFields.addAll(explorePaths(nextPathExplorer.getStack()
                    .getSingleCurrentConditionalStmtsAfter(prevCondStmt), valueStates, alreadyAnalysed));
        }
        return obtainedFields;
    }

    static <T extends ValueState> List<T> getNextPathValues(ConditionalStmt conditionalStmt, List<T> valueStates) {
        List<T> nextPathValues = new ArrayList<>();
        for (T valueState : valueStates) {
            if (valueState.getStack().hasCondStmtEqualTo(conditionalStmt))
                nextPathValues.add(valueState);
        }
        return nextPathValues;
    }
}
