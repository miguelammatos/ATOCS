package atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import atocs.core.datastructures.InvokeExprState;
import atocs.core.datastructures.ValueState;

import java.util.ArrayList;
import java.util.List;

import static atocs.core.Constants.*;

public class NativeJavaAnalyser {
    private static final Logger logger = LoggerFactory.getLogger(NativeJavaAnalyser.class);

    /**
     * Determines if the valueState provided is an object representation of a Java primitive type. Ex: Integer, Float,
     * Double, etc.
     *
     * @param valueState object reference.
     * @return the name of the Java Object or null the reference is not a Java primitive object type.
     */
    static String isJavaPrimitiveObject(ValueState valueState) {
        if (CodeAnalyser.isOfType(valueState, INTEGER_CLASS))
            return INTEGER_CLASS;
        else if (CodeAnalyser.isOfType(valueState, SHORT_CLASS))
            return SHORT_CLASS;
        else if (CodeAnalyser.isOfType(valueState, LONG_CLASS))
            return LONG_CLASS;
        else if (CodeAnalyser.isOfType(valueState, DOUBLE_CLASS))
            return DOUBLE_CLASS;
        else if (CodeAnalyser.isOfType(valueState, FLOAT_CLASS))
            return FLOAT_CLASS;
        else if (CodeAnalyser.isOfType(valueState, BOOLEAN_CLASS))
            return BOOLEAN_CLASS;
        else if (CodeAnalyser.isOfType(valueState, CHAR_CLASS))
            return CHAR_CLASS;
        else if (CodeAnalyser.isOfType(valueState, BYTE_CLASS))
            return BYTE_CLASS;
        return null;
    }

    /**
     * Determines if the valueState provided is an object representation of a Java primitive type. Ex: Integer, Float,
     * Double, etc.
     *
     * @param invokeExprState invoke expression reference.
     * @return the name of the Java Object or null the reference is not a Java primitive object type.
     */
    static boolean isJavaPrimitiveValueMethod(InvokeExprState invokeExprState) {
        if (isJavaPrimitiveObject(invokeExprState.getInstance()) != null) {
            String methodName = invokeExprState.getMethodName();
            switch (methodName) {
                case INTEGER_CLASS_VALUE_METHOD:
                case SHORT_CLASS_VALUE_METHOD:
                case LONG_CLASS_VALUE_METHOD:
                case DOUBLE_CLASS_VALUE_METHOD:
                case FLOAT_CLASS_VALUE_METHOD:
                case BOOLEAN_CLASS:
                case CHAR_CLASS_VALUE_METHOD:
                case BYTE_CLASS_VALUE_METHOD:
                    return true;
            }
        }
        return false;
    }

    /**
     * Obtains the value associated with a Java object representation of a primitive type.
     *
     * @param objRef reference to the Java object.
     * @return the possible values associated with the provided object.
     */
    static List<ValueState> getValueFromJavaPrimitiveObject(ValueState objRef) {
        List<ValueState> values = new ArrayList<>();
        String objectClass = isJavaPrimitiveObject(objRef);
        if (objectClass != null) {

            List<InvokeExprState> initExprs = CodeAnalyser.findObjConstructorInvocationFromObjRef(objRef);
            for (InvokeExprState initExpr : initExprs) {
                if (initExpr.getArgCount() == 1) {
                    values.add(initExpr.getArg(0));
                }
            }
            List<InvokeExprState> valueOfExprs = CodeAnalyser.findMethodInvocationAssignedToVariable(objectClass,
                    VALUE_OF_METHOD, objRef);
            for (InvokeExprState valueOfExpr : valueOfExprs) {
                if (valueOfExpr.getArgCount() == 1) {
                    values.add(valueOfExpr.getArg(0));
                }
            }
        }
        return values;
    }

    /**
     * Obtains a collection class name from a ValueState.
     *
     * @param valueState collection reference.
     * @return the name of the Java Collection or null the reference is not a collection.
     */
    static String isCollectionClass(ValueState valueState) {
        if (CodeAnalyser.isOfType(valueState, JAVA_LIST))
            return JAVA_LIST;
        else if (CodeAnalyser.isOfType(valueState, JAVA_SET))
            return JAVA_SET;
        else if (CodeAnalyser.isOfType(valueState, JAVA_ITERATOR))
            return JAVA_ITERATOR;
        else if (CodeAnalyser.isOfType(valueState, JAVA_MAP))
            return JAVA_MAP;
        return null;
    }

    /**
     * Determines if the method is a Java Collection method and returns the collection class name.
     *
     * @param invokeExprState method invocation.
     * @return the name of the Java Collection or null if the method is not from a Collection.
     */
    static String isCollectionMethod(InvokeExprState invokeExprState) {
        if (invokeExprState.hasInstance()) {
            if (CodeAnalyser.isOfType(invokeExprState.getInstance(), JAVA_LIST))
                return JAVA_LIST;
            else if (CodeAnalyser.isOfType(invokeExprState.getInstance(), JAVA_SET))
                return JAVA_SET;
            else if (CodeAnalyser.isOfType(invokeExprState.getInstance(), JAVA_ITERATOR))
                return JAVA_ITERATOR;
            else if (CodeAnalyser.isOfType(invokeExprState.getInstance(), JAVA_MAP))
                return JAVA_MAP;
        }
        return null;
    }

    /**
     * Obtains the object references added to a Java Collection. Supports List and Set.
     *
     * @param collectionRefState collection reference state.
     * @return all object references added to this collection, if any.
     */
    static List<ValueState> getObjsAddedToCollection(ValueState collectionRefState) {
        String collectionName = isCollectionClass(collectionRefState);
        if (collectionName != null)
            return getObjsAddedToCollection(collectionName, collectionRefState);
        else
            return new ArrayList<>();
    }

    /**
     * Obtains the object references added to a Java Collection. Supports List and Set.
     *
     * @param collection collection class name.
     * @param collectionRefState collection reference state.
     * @return all object references added to a List, if any.
     */
    static List<ValueState> getObjsAddedToCollection(String collection, ValueState collectionRefState) {
        switch (collection) {
            case JAVA_LIST:
                return getObjsAddedToList(collectionRefState);
            case JAVA_SET:
                return getObjsAddedToSet(collectionRefState);
            case JAVA_ITERATOR:
                return getObjsAddedToCollectionFromIterator(collectionRefState);
            case JAVA_MAP:
                return getObjsAddedToMap(collectionRefState);
            default:
                logger.warn("Unsupported Java Collection " + collection);
                return new ArrayList<>();
        }
    }

    /**
     * Obtains the object references added to a List. Supports List and ArrayList with the following methods: add,
     * set and addAll.
     *
     * @param listRefState list reference state.
     * @return all object references added to a List, if any.
     */
    static List<ValueState> getObjsAddedToList(ValueState listRefState) {
        List<ValueState> objsAdded = new ArrayList<>();

        List<ValueState> listRefs = CodeAnalyser.getAllObjRefsFromSingleRef(listRefState);
        List<InvokeExprState> initMethodInvokes = new ArrayList<>();
        List<InvokeExprState> addAllMethodInvokes = new ArrayList<>();
        List<InvokeExprState> addAndSetMethodInvokes = new ArrayList<>();
        // For each list object reference find different method invocations to analyse
        for (ValueState listRef : listRefs) {
            initMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(INIT_METHOD, listRef,
                    new ArrayList<>()));
            addAllMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_LIST_ADD_ALL_METHOD,
                    listRef, new ArrayList<>()));
            addAndSetMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_LIST_ADD_METHOD,
                    listRef, new ArrayList<>()));
            addAndSetMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_LIST_SET_METHOD,
                    listRef, new ArrayList<>()));
        }

        List<ValueState> otherCollectionsToAnalyse = new ArrayList<>();
        for (InvokeExprState initMethodInvoke : initMethodInvokes) {
            if (initMethodInvoke.getArgCount() == 1 && isCollectionClass(initMethodInvoke.getArg(0)) != null)
                otherCollectionsToAnalyse.add(initMethodInvoke.getArg(0));
        }
        for (InvokeExprState addAllMethodInvoke : addAllMethodInvokes) {
            if (addAllMethodInvoke.getArgCount() == 1)
                otherCollectionsToAnalyse.add(addAllMethodInvoke.getArg(0));
            else if (addAllMethodInvoke.getArgCount() == 2)
                otherCollectionsToAnalyse.add(addAllMethodInvoke.getArg(1));
            else
                logger.warn("Unknown List " + JAVA_LIST_ADD_ALL_METHOD + " method");
        }
        for (InvokeExprState addOrSetMethodInvoke : addAndSetMethodInvokes) {
            if (addOrSetMethodInvoke.getArgCount() == 1)
                objsAdded.add(addOrSetMethodInvoke.getArg(0));
            else if (addOrSetMethodInvoke.getArgCount() == 2)
                objsAdded.add(addOrSetMethodInvoke.getArg(1));
            else
                logger.warn("Unknown List " + JAVA_LIST_ADD_METHOD + " or " + JAVA_LIST_SET_METHOD + " method");
        }
        for (ValueState collection : otherCollectionsToAnalyse) {
            objsAdded.addAll(getObjsAddedToCollection(collection));
        }

        return objsAdded;
    }

    /**
     * Obtains the object references added to a Set. Supports Set, HashSet and TreeSet with the following methods: add,
     * set and addAll.
     *
     * @param setRefState set reference state.
     * @return all object references added to a Set, if any.
     */
    static List<ValueState> getObjsAddedToSet(ValueState setRefState) {
        List<ValueState> objsAdded = new ArrayList<>();

        List<ValueState> setRefs = CodeAnalyser.getAllObjRefsFromSingleRef(setRefState);
        List<InvokeExprState> initMethodInvokes = new ArrayList<>();
        List<InvokeExprState> addAllMethodInvokes = new ArrayList<>();
        List<InvokeExprState> addMethodInvokes = new ArrayList<>();
        // For each list object reference find different method invocations to analyse
        for (ValueState setRef : setRefs) {
            initMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(INIT_METHOD, setRef,
                    new ArrayList<>()));
            addAllMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_SET_ADD_ALL_METHOD,
                    setRef, new ArrayList<>()));
            addMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_SET_ADD_METHOD,
                    setRef, new ArrayList<>()));
        }

        List<ValueState> otherCollectionsToAnalyse = new ArrayList<>();
        for (InvokeExprState initMethodInvoke : initMethodInvokes) {
            if (initMethodInvoke.getArgCount() == 1 && isCollectionClass(initMethodInvoke.getArg(0)) != null)
                otherCollectionsToAnalyse.add(initMethodInvoke.getArg(0));
        }
        for (InvokeExprState addAllMethodInvoke : addAllMethodInvokes) {
            if (addAllMethodInvoke.getArgCount() == 1)
                otherCollectionsToAnalyse.add(addAllMethodInvoke.getArg(0));
            else if (addAllMethodInvoke.getArgCount() == 2)
                otherCollectionsToAnalyse.add(addAllMethodInvoke.getArg(1));
            else
                logger.warn("Unknown Set " + JAVA_SET_ADD_ALL_METHOD + " method");
        }
        for (InvokeExprState addMethodInvoke : addMethodInvokes) {
            if (addMethodInvoke.getArgCount() == 1)
                objsAdded.add(addMethodInvoke.getArg(0));
            else
                logger.warn("Unknown Set " + JAVA_SET_ADD_METHOD + " method");
        }
        for (ValueState collection : otherCollectionsToAnalyse) {
            objsAdded.addAll(getObjsAddedToCollection(collection));
        }

        return objsAdded;
    }

    /**
     * Obtains the Collection reference corresponding to the Iterator provided. Obtain the objects added to the
     * Collection.
     *
     * @param itRefState Iterator reference.
     * @return the values added to the Collections linked to the provided iterator.
     */
    static List<ValueState> getObjsAddedToCollectionFromIterator(ValueState itRefState) {
        List<ValueState> objsAdded = new ArrayList<>();

        for (InvokeExprState listGetIteratorExpr : CodeAnalyser.findMethodInvocationAssignedToVariable(JAVA_LIST,
                JAVA_ITERATOR_METHOD, itRefState)) { // find list.iterator() method
            if (listGetIteratorExpr.hasInstance()) {
                objsAdded.addAll(getObjsAddedToCollection(JAVA_LIST, listGetIteratorExpr.getInstance()));
            }
        }
        for (InvokeExprState setGetIteratorExpr : CodeAnalyser.findMethodInvocationAssignedToVariable(JAVA_SET,
                JAVA_ITERATOR_METHOD, itRefState)) { // find set.iterator() method
            if (setGetIteratorExpr.hasInstance()) {
                objsAdded.addAll(getObjsAddedToCollection(JAVA_SET, setGetIteratorExpr.getInstance()));
            }
        }

        return objsAdded;
    }

    /**
     * Obtains the object references added to a Map. Supports Map with the following methods: put, putAll and
     * putIfAbsent.
     * Only returns the values added to the map and not the keys
     *
     * @param mapRefState map reference state.
     * @return all object references added to a Map, if any.
     */
    static List<ValueState> getObjsAddedToMap(ValueState mapRefState) {
        List<ValueState> objsAdded = new ArrayList<>();

        List<ValueState> mapRefs = CodeAnalyser.getAllObjRefsFromSingleRef(mapRefState);
        List<InvokeExprState> initMethodInvokes = new ArrayList<>();
        List<InvokeExprState> putAndPutIfAbsentMethodInvokes = new ArrayList<>();
        List<InvokeExprState> putAllMethodInvokes = new ArrayList<>();
        List<InvokeExprState> replaceMethodInvokes = new ArrayList<>();
        // For each map object reference find different method invocations to analyse
        for (ValueState mapRef : mapRefs) {
            initMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(INIT_METHOD, mapRef,
                    new ArrayList<>()));
            putAndPutIfAbsentMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_MAP_PUT,
                    mapRef, new ArrayList<>()));
            putAndPutIfAbsentMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(
                    JAVA_MAP_PUT_IF_ABSENT, mapRef, new ArrayList<>()));
            putAllMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_MAP_PUT_ALL,
                    mapRef, new ArrayList<>()));
            replaceMethodInvokes.addAll(CodeAnalyser.findMethodInvocationFromSingleObjectRef(JAVA_MAP_REPLACE,
                    mapRef, new ArrayList<>()));
        }

        List<ValueState> otherCollectionsToAnalyse = new ArrayList<>();
        for (InvokeExprState initMethodInvoke : initMethodInvokes) {
            if (initMethodInvoke.getArgCount() == 1 && isCollectionClass(initMethodInvoke.getArg(0)) != null)
                otherCollectionsToAnalyse.add(initMethodInvoke.getArg(0));
        }
        for (InvokeExprState putAndPutIfAbsentMethodInvoke : putAndPutIfAbsentMethodInvokes) {
            if (putAndPutIfAbsentMethodInvoke.getArgCount() == 2)
                objsAdded.add(putAndPutIfAbsentMethodInvoke.getArg(1));
            else
                logger.warn("Unknown Map " + JAVA_MAP_PUT + " or " + JAVA_MAP_PUT_IF_ABSENT + " method");
        }
        for (InvokeExprState putAllMethodInvoke : putAllMethodInvokes) {
            if (putAllMethodInvoke.getArgCount() == 1 && isCollectionClass(putAllMethodInvoke.getArg(0)) != null)
                otherCollectionsToAnalyse.add(putAllMethodInvoke.getArg(0));
            else
                logger.warn("Unknown Map " + JAVA_MAP_PUT_ALL + " method");
        }
        for (InvokeExprState replaceMethodInvoke : replaceMethodInvokes) {
            if (replaceMethodInvoke.getArgCount() == 2)
                objsAdded.add(replaceMethodInvoke.getArg(1));
            else if (replaceMethodInvoke.getArgCount() == 3)
                objsAdded.add(replaceMethodInvoke.getArg(2));
            else
                logger.warn("Unknown Map " + JAVA_MAP_REPLACE + " method");
        }
        for (ValueState collection : otherCollectionsToAnalyse) {
            objsAdded.addAll(getObjsAddedToCollection(collection));
        }

        return objsAdded;
    }

}
