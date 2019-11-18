import liveAnalysis.AnalysisTransformer;
import soot.*;

public class RunDataFlowAnalysis
{
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: java RunDataFlowAnalysis class_to_analyse");
            System.exit(1);
        } else {
            System.out.println("Analyzing class: "+args[0]);
        }

        String mainClass = args[0];

        // You may have to update the class Path based on your OS and Java version
        /*** *** YOU SHOULD EDIT THIS BEFORE RUNNING *** ***/
        String classPath = "target/classes:" ;
//                "/Users/david/projects/local_thesis/soot/jasminclasses-2.5.0.jar:" +
//                "/Users/david/projects/local_thesis/soot/polyglotclasses-1.3.5.jar:" +
//                "/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/rt.jar:" +
//                "/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jce.jar:"; 			//change to appropriate path to the test class
        // if needed add path to rt.jar (or classes.jar)


        //Set up arguments for Soot
        String[] sootArgs = {
                "-cp", classPath, "-pp", 	// sets the class path for Soot
                "-w", 						// Whole program analysis, necessary for using Transformer
                "-src-prec", "only-class",		// Specify type of source file
                "-main-class", mainClass,	// Specify the main class
                "-f", "J", 					// Specify type of output file
                mainClass
        };

        // Create transformer for analysis
        AnalysisTransformer analysisTransformer = new AnalysisTransformer();

        // Add transformer to appropriate Pack in PackManager. PackManager will run all Packs when main function of Soot is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.dfa", analysisTransformer));

        // Call main function with arguments
        soot.Main.main(sootArgs);

    }
}

