package lu.uni.trux.jucify;

import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.javatuples.Pair;
import org.slf4j.profiler.StopWatch;

import lu.uni.trux.jucify.callgraph.CallGraphPatcher;
import lu.uni.trux.jucify.utils.CommandLineOptions;
import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.CustomPrints;
import lu.uni.trux.jucify.utils.Utils;
import soot.Scene;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SootIntegrationMode;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;

/*-
 * #%L
 * JuCify
 *
 * %%
 * Copyright (C) 2021 Jordan Samhi
 * University of Luxembourg - Interdisciplinary Centre for
 * Security Reliability and Trust (SnT) - TruX - All rights reserved
 *
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

public class Main {
	public static void main(String[] args) throws Throwable {
		StopWatch analysisTime = new StopWatch("Analysis");
		analysisTime.start("Analysis");
		CommandLineOptions options = new CommandLineOptions(args);

		if(!options.hasRaw()) {
			System.out.println(String.format("%s v%s started on %s\n", Constants.JUCIFY, Constants.VERSION, new Date()));
		}

		String apk = options.getApk(),
				platforms = options.getPlatforms();
		List<Pair<String, String>> files = options.getFiles();
		InfoflowAndroidConfiguration ifac = new InfoflowAndroidConfiguration();
		ifac.getAnalysisFileConfig().setAndroidPlatformDir(platforms);
		ifac.getAnalysisFileConfig().setTargetAPKFile(apk);
		SetupApplication sa = new SetupApplication(ifac);
		sa.constructCallgraph();
		CallGraph cg = Scene.v().getCallGraph();
		
		if(options.hasExportCallBeforeProcessingGraphTxt()) {
			Utils.exportCallGraphTxT(cg, options.getExportCallGraphBeforeProcessingTxtDestination());
		}

		ProcessManifest pm = new  ProcessManifest(apk);
		if(!options.hasRaw()) {
			CustomPrints.pinfo(String.format("Processing: %s", pm.getPackageName()));
		}

		if(!options.hasRaw()) {
			CustomPrints.pinfo("Loading binary call-graphs + java-to-native and native-to-java links...");
		}

		int sizeCallGraphBeforePatch = cg.size();


		StopWatch instrumentationTime = new StopWatch("Instrumentation");
		instrumentationTime.start("Instrumentation");
		
		ResultsAccumulator.v().setNumberEdgesBeforeJucify(Utils.getNumberOfEdgesInCG(cg));
		ResultsAccumulator.v().setNumberNodesBeforeJucify(Utils.getNumberOfNodesInCG(cg));
		
		CallGraphPatcher cgp = new CallGraphPatcher(cg, options.hasRaw());
		cgp.importBinaryCallGraph(files, !options.isSymbolicGenerationDisabled());
		if(!options.hasRaw()) {
			CustomPrints.psuccess("Binary callgraph imported.");
		}
		instrumentationTime.stop();
		ResultsAccumulator.v().setInstrumentationElapsedTime(instrumentationTime.elapsedTime() / 1000000000);

		int sizeCallGraphAfterPatch = cg.size();

		sa.getConfig().setSootIntegrationMode(SootIntegrationMode.UseExistingInstance);
		sa.getConfig().getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Precise);
		sa.getConfig().setCodeEliminationMode(CodeEliminationMode.NoCodeElimination);

		StopWatch taintAnalysisTime = new StopWatch("Taint Analysis");
		taintAnalysisTime.start("Taint Analysis");
		if(options.hasTaintAnalysis()) {
			if(!options.hasRaw()) {
				CustomPrints.pinfo("Taint Analysis in progress...");
			}
			FlowAnalysis fa = new  FlowAnalysis(sa, options.hasRaw());
			fa.run();
			if(!options.hasRaw()) {
				CustomPrints.psuccess("Taint Analysis performed.");
			}
		}
		taintAnalysisTime.stop();
		ResultsAccumulator.v().setTaintAnalysisElapsedTime(taintAnalysisTime.elapsedTime() / 1000000000);

		if(options.hasExportCallGraph()) {
			String destination = options.getExportCallGraphDestination();
			if(!options.hasRaw()) {
				CustomPrints.pinfo(String.format("Exporting call graph to %s...", destination));
			}
			cgp.dotifyCallGraph(destination);
			if(!options.hasRaw()) {
				CustomPrints.psuccess("Callgraph exported.");
			}
		}

		if(options.hasExportCallGraphTxt()) {
			Utils.exportCallGraphTxT(cg, options.getExportCallGraphTxtDestination());
		}
		
		pm.close();

		analysisTime.stop();
		ResultsAccumulator.v().setAppName(FilenameUtils.getBaseName(options.getApk()));
		ResultsAccumulator.v().setAnalysisElapsedTime(analysisTime.elapsedTime() / 1000000000);
		ResultsAccumulator.v().setNumberNewCallGraphReachableNodes(cgp.getNewReachableNodesNative().size() + cgp.getNewReachableNodesJava().size());
		ResultsAccumulator.v().setNumberNewCallGraphReachableNodesNative(cgp.getNewReachableNodesNative().size());
		ResultsAccumulator.v().setNumberNewCallGraphReachableNodesJava(cgp.getNewReachableNodesJava().size());
		ResultsAccumulator.v().setNumberNewEdges(sizeCallGraphAfterPatch - sizeCallGraphBeforePatch);
		ResultsAccumulator.v().setNumberEdgesAfterJucify(Utils.getNumberOfEdgesInCG(cg));
		ResultsAccumulator.v().setNumberNodesAfterJucify(Utils.getNumberOfNodesInCG(cg));
		if(options.hasRaw()) {
			ResultsAccumulator.v().printVectorResults();
		}else {
			ResultsAccumulator.v().printResults();
		}
	}
}
