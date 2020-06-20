package atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import atocs.core.api.API;
import atocs.core.exceptions.SystemException;
import soot.jimple.SwitchStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AnalysisManager extends SceneTransformer {
    private final Logger logger = LoggerFactory.getLogger(AnalysisManager.class);
    private final AtocsConfig atocsConfig;

    AnalysisManager(AtocsConfig atocsConfig) {
        super();
        this.atocsConfig = atocsConfig;
    }

    /**
     * Internally called by soot to perform the analysis. It is the starting point of the system.
     */
    @Override
    protected void internalTransform(String arg0, Map arg1) {
        try {
            execute();
        } catch (SystemException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Creates an API from the provided configuration file and executes the code analysis with this information.
     *
     * @throws SystemException when any type of error occurs.
     */
    void execute() throws SystemException {
        API.getInstance().init(atocsConfig.getApiFilePath());
        atocsConfig.getDatabasePlugin().setApi();

        Inspector inspector = new Inspector();
        Set<SootMethod> methodsToAnalyse = inspector.getMethodsWithDbInteractions();

        CodeAnalyser analyser = new CodeAnalyser(atocsConfig.getDatabasePlugin());
        analyser.analyse(methodsToAnalyse);

        Configurator.getInstance().showReport();
        System.out.println("ATOCS final output is presented in the " + Constants.REPORT_FILE_NAME + " file.");


    }

    void test() {
        SootMethod m = Scene.v().getSootClass("Db").getMethodByName("increment");
        UnitGraph graph = new BriefUnitGraph(m.getActiveBody());
        Iterator<Unit> unitIt = graph.iterator();

        while (unitIt.hasNext()) {
            Unit unit = unitIt.next();
            System.out.println(unit);
//            System.out.println(graph.getPredsOf(unit));
//            System.out.println(graph.getSuccsOf(unit));
            System.out.println("");
        }

//        CallGraph cg = Scene.v().getCallGraph();
//        Iterator targets = new Targets(cg.edgesOutOf(m));
//        while (targets.hasNext()) {
//            SootMethod me = (SootMethod) targets.next();
//            System.out.println("TARGET: " + me.getDeclaringClass().getName() + " " + me.getName());
//        }

    }

}