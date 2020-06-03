package atocs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.options.Options;
import atocs.core.exceptions.SystemException;

import java.util.ArrayList;
import java.util.List;

public class Main
{
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("ERROR: Program arguments not correct.");
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"configFilePath\"");
            System.out.println("Note: The configuration file must respect the system standard.");
            System.exit(1);
        }

        try {
            executeAnalysis(Parser.getInstance().parseConfigFile(args[0]));
        } catch (SystemException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    static void executeAnalysis(AtocsConfig atocsConfig) {
        List<String> list = new ArrayList<>();
        list.add("-cp"); list.add(atocsConfig.getClassPath()); // sets the class path for Soot
        list.add("-pp"); // Prepend the given soot classpath to the default classpath
        list.add("-f"); list.add("n"); // Do not generate sootOutput
        list.add("-w"); // Whole program analysis, necessary for using Transformer
        list.add("-src-prec"); list.add("class"); // Specify type of source file. Possibly use only-class
        list.add("-allow-phantom-refs"); // So that the program still runs in case some classes are not found
        for (String directory : atocsConfig.getDirectoriesToAnalyse()) {
            list.add("-process-dir");
            list.add(directory);
        }

        AnalysisManager analysisManager = new AnalysisManager(atocsConfig);

        // Add transformer to appropriate Pack in PackManager. PackManager will run all Packs when main function of Soot is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.dfa", analysisManager));

        Options.v().parse(list.toArray(new String[list.size()]));
        Scene.v().loadNecessaryClasses();

        if (!atocsConfig.getEntryPoints().isEmpty()) {
            List<SootMethod> entryPoints = new ArrayList<>();
            for (String entryPoint : atocsConfig.getEntryPoints())
                entryPoints.add(Scene.v().getSootClass(entryPoint).getMethodByName("main"));
            Scene.v().setEntryPoints(entryPoints);
        }

        PackManager.v().runPacks();
    }

}

