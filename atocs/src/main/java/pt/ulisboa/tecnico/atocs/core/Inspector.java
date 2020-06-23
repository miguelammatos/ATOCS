package pt.ulisboa.tecnico.atocs.core;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import pt.ulisboa.tecnico.atocs.core.api.API;

import java.util.*;

public class Inspector {
    /**
     * Searches the application code to gather all the functions that have any interaction with
     * the database API inside them.
     *
     * @return methods with database interactions.
     */
    Set<SootMethod> getMethodsWithDbInteractions() {
        Set<SootMethod> methodsToAnalyse = new HashSet<>();
        CallGraph cg = Scene.v().getCallGraph();
        Set<SootMethod> interactionMethods = API.getInstance().getMethods();
        for (SootMethod interactionMethod : interactionMethods) {
            Iterator<MethodOrMethodContext> sources = new Sources(cg.edgesInto(interactionMethod));
            while (sources.hasNext()) {
                SootMethod src = (SootMethod) sources.next();
                //ensure that the we only analyse application methods and not library methods
                if (CodeAnalyser.assertMethodAnalysis(src)) {
                    methodsToAnalyse.add(src);
                }
            }
        }
        return methodsToAnalyse;
    }
}
