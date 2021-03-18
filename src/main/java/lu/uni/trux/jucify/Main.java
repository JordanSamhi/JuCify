package lu.uni.trux.jucify;

import java.util.Date;
import java.util.List;

import org.javatuples.Pair;

import lu.uni.trux.jucify.callgraph.CallGraphPatcher;
import lu.uni.trux.jucify.utils.CommandLineOptions;
import lu.uni.trux.jucify.utils.Constants;
import soot.Scene;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SootIntegrationMode;
import soot.jimple.infoflow.android.SetupApplication;
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
		System.out.println(String.format("%s v%s started on %s\n", Constants.JUCIFY, Constants.VERSION, new Date()));
		CommandLineOptions options = new CommandLineOptions(args);
		String apk = options.getApk(),
				platforms = options.getPlatforms();
		List<Pair<String, String>> files = options.getFiles();
		InfoflowAndroidConfiguration ifac = new InfoflowAndroidConfiguration();
		ifac.getAnalysisFileConfig().setAndroidPlatformDir(platforms);
		ifac.getAnalysisFileConfig().setTargetAPKFile(apk);
		SetupApplication sa = new SetupApplication(ifac);
		sa.constructCallgraph();
		CallGraph cg = Scene.v().getCallGraph();


		System.out.println("Loading binary call-graphs + java-to-native and native-to-java links...");
		CallGraphPatcher cgp = new CallGraphPatcher(cg);
		cgp.importBinaryCallGraph(files);
		System.out.println("Binary callgraph imported.");

		sa.getConfig().setSootIntegrationMode(SootIntegrationMode.UseExistingInstance);
		sa.getConfig().getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Precise);

		if(options.hasTaintAnalysis()) {
			System.out.println("Taint Analysis in progress...");
			FlowAnalysis fa = new  FlowAnalysis(sa);
			fa.run();
			System.out.println("Taint Analysis performed.");
		}

		if(options.hasExportCallGraph()) {
			String destination = options.getExportCallGraphDestination();
			System.out.println(String.format("Exporting call graph to %s...", destination));
			cgp.dotifyCallGraph(destination);
			System.out.println("Callgraph exported.");
		}
	}
}
