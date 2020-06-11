package atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import atocs.core.api.API;
import atocs.core.exceptions.SystemException;

import java.util.Map;
import java.util.Set;

public class AnalysisManager extends SceneTransformer {
    private final Logger logger = LoggerFactory.getLogger(AnalysisManager.class);
    private static String REPORT_FILE_NAME = "atocs-output.txt";
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

        Configurator.getInstance().showReport(REPORT_FILE_NAME);
        System.out.println("ATOCS final output is presented in the " + REPORT_FILE_NAME + " file.");
    }

}