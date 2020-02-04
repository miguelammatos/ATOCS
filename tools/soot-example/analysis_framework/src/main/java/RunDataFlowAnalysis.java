import liveAnalysis.AnalysisTransformer;
import soot.*;

public class RunDataFlowAnalysis
{
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: mvn compile exec:java -Dexec.args=\"directory-to-analyse classpath\"");
            System.exit(1);
        } else {
            System.out.println("Analyzing directory: " + args[0]);
        }

        String directory = args[0];
        String classPath = args[1];

        String[] sootArgs = {
                "-cp", classPath, "-pp", 	// sets the class path for Soot
                "-w", 						// Whole program analysis, necessary for using Transformer
                "-src-prec", "class",		// Specify type of source file. Possibly use only-class
//                "-f", "J", 					// Specify type of output file
                "-allow-phantom-refs",
                "-process-dir", directory
        };

        AnalysisTransformer analysisTransformer = new AnalysisTransformer();

        // Add transformer to appropriate Pack in PackManager. PackManager will run all Packs when main function of Soot is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.dfa", analysisTransformer));

        // Call main function with arguments
        soot.Main.main(sootArgs);

    }
}

