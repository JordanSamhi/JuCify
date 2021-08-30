package lu.uni.trux.jucify.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.javatuples.Pair;
import org.javatuples.Triplet;

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

/**
 * This class sets the different option for the application
 * @author Jordan Samhi
 *
 */
public class CommandLineOptions {

	private static final Triplet<String, String, String> DIS_SYMBOLIC_GEN = new Triplet<String, String, String>("disable-symbolic-gen", "s", "Disable symbolic code generation");
	private static final Triplet<String, String, String> APK = new Triplet<String, String, String>("apk", "a", "Apk file");
	private static final Triplet<String, String, String> HELP = new Triplet<String, String, String>("help", "h", "Print this message");
	private static final Triplet<String, String, String> FILES = new Triplet<String, String, String>("files", "f", "Specify the files to load (dot file -> entrypoint file)");
	private static final Triplet<String, String, String> EXPORT_CG = new Triplet<String, String, String>("export-cg-to-dot", "e", "Export call graph to dot file");
	private static final Triplet<String, String, String> EXPORT_CG_TXT = new Triplet<String, String, String>("export-cg-to-txt", "c", "Export call graph to txt file");
	private static final Triplet<String, String, String> EXPORT_CG_BEFORE_PROCESSING_TXT = new Triplet<String, String, String>("export-cg-before-processing-to-txt", "b", "Export call graph before being processed by JuCify to txt file");
	private static final Triplet<String, String, String> TIMEOUT = new Triplet<String, String, String>("timeout", "t", "Set the timeout for analysis");
	private static final Triplet<String, String, String> TAINT_ANALYSIS = new Triplet<String, String, String>("taintanalysis", "ta", "Run taint analysis on APK");
	private static final Triplet<String, String, String> PLATFORMS =
			new Triplet<String, String, String>("platforms", "p", "Android platforms folder");
	private static final Triplet<String, String, String> RAW = new Triplet<String, String, String>("raw", "r", "Print raw results");

	private Options options, firstOptions;
	private CommandLineParser parser;
	private CommandLine cmdLine, cmdFirstLine;

	public CommandLineOptions(String[] args) {
		this.options = new Options();
		this.firstOptions = new Options();
		this.initOptions();
		this.parser = new DefaultParser();
		this.parse(args);
	}

	/**
	 * This method does the parsing of the arguments.
	 * It distinguished, real options and help option.
	 * @param args the arguments of the application
	 */
	private void parse(String[] args) {
		HelpFormatter formatter = null;
		try {
			this.cmdFirstLine = this.parser.parse(this.firstOptions, args, true);
			if (this.cmdFirstLine.hasOption(HELP.getValue0())) {
				formatter = new HelpFormatter();
				formatter.printHelp(Constants.JUCIFY, this.options, true);
				System.exit(0);
			}
			this.cmdLine = this.parser.parse(this.options, args);
		} catch (ParseException e) {
			CustomPrints.perror(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Initialization of all recognized options
	 */
	private void initOptions() {
		final Option dis_symbolic_gen = Option.builder(DIS_SYMBOLIC_GEN.getValue1())
				.longOpt(DIS_SYMBOLIC_GEN.getValue0())
				.desc(DIS_SYMBOLIC_GEN.getValue2())
				.hasArg(false)
				.argName(DIS_SYMBOLIC_GEN.getValue0())
				.required(false)
				.build();

		final Option apk = Option.builder(APK.getValue1())
				.longOpt(APK.getValue0())
				.desc(APK.getValue2())
				.hasArg(true)
				.argName(APK.getValue0())
				.required(true)
				.build();
		
		final Option files = Option.builder(FILES.getValue1())
				.longOpt(FILES.getValue0())
				.desc(FILES.getValue2())
				.hasArg(true)
				.argName(FILES.getValue0())
				.required(false)
				.build();
		
		final Option raw = Option.builder(RAW.getValue1())
				.longOpt(RAW.getValue0())
				.desc(RAW.getValue2())
				.hasArg(false)
				.argName(RAW.getValue0())
				.required(false)
				.build();
		
		final Option to = Option.builder(TIMEOUT.getValue1())
				.longOpt(TIMEOUT.getValue0())
				.desc(TIMEOUT.getValue2())
				.hasArg(true)
				.argName(TIMEOUT.getValue0())
				.required(false)
				.build();
		
		final Option ta = Option.builder(TAINT_ANALYSIS.getValue1())
				.longOpt(TAINT_ANALYSIS.getValue0())
				.desc(TAINT_ANALYSIS.getValue2())
				.hasArg(false)
				.required(false)
				.build();
		
		final Option export = Option.builder(EXPORT_CG.getValue1())
				.longOpt(EXPORT_CG.getValue0())
				.desc(EXPORT_CG.getValue2())
				.hasArg(true)
				.argName(EXPORT_CG.getValue0())
				.required(false)
				.build();
		
		final Option export_cg_txt = Option.builder(EXPORT_CG_TXT.getValue1())
				.longOpt(EXPORT_CG_TXT.getValue0())
				.desc(EXPORT_CG_TXT.getValue2())
				.hasArg(true)
				.argName(EXPORT_CG_TXT.getValue0())
				.required(false)
				.build();
		
		final Option export_cg_before_processing_txt = Option.builder(EXPORT_CG_BEFORE_PROCESSING_TXT.getValue1())
				.longOpt(EXPORT_CG_BEFORE_PROCESSING_TXT.getValue0())
				.desc(EXPORT_CG_BEFORE_PROCESSING_TXT.getValue2())
				.hasArg(true)
				.argName(EXPORT_CG_BEFORE_PROCESSING_TXT.getValue0())
				.required(false)
				.build();

		final Option platforms = Option.builder(PLATFORMS.getValue1())
				.longOpt(PLATFORMS.getValue0())
				.desc(PLATFORMS.getValue2())
				.hasArg(true)
				.argName(PLATFORMS.getValue0())
				.required(true)
				.build();

		final Option help = Option.builder(HELP.getValue1())
				.longOpt(HELP.getValue0())
				.desc(HELP.getValue2())
				.argName(HELP.getValue0())
				.build();

		this.firstOptions.addOption(help);

		this.options.addOption(dis_symbolic_gen);
		this.options.addOption(apk);
		this.options.addOption(platforms);
		this.options.addOption(to);
		this.options.addOption(files);
		this.options.addOption(export);
		this.options.addOption(export_cg_txt);
		this.options.addOption(raw);
		this.options.addOption(ta);
		this.options.addOption(export_cg_before_processing_txt);

		for(Option o : this.firstOptions.getOptions()) {
			this.options.addOption(o);
		}
	}

	public String getApk() {
		return this.cmdLine.getOptionValue(APK.getValue0());
	}

	public String getPlatforms() {
		return this.cmdLine.getOptionValue(PLATFORMS.getValue0());
	}
	
	public List<Pair<String, String>> getFiles() {
		String files = this.cmdLine.getOptionValue(FILES.getValue0());
		List<Pair<String, String>> pairs = new ArrayList<Pair<String,String>>();
        if(files != null){
            for(String pair: files.split("\\|")) {
                if(! pair.isEmpty()) {
                    String[] split = pair.split(":");
                    if(split.length == 2) {
                        pairs.add(new Pair<String, String>(split[0], split[1]));
                    }else {
                        CustomPrints.perror(String.format("Files not valid: $s", pair));
                    }
                }
            }
        }
		return pairs;
	}
	
	public boolean isSymbolicGenerationDisabled() {
		return this.cmdLine.hasOption(DIS_SYMBOLIC_GEN.getValue1());
	}
	
	public String getExportCallGraphDestination() {
		return this.cmdLine.getOptionValue(EXPORT_CG.getValue0());
	}
	
	public boolean hasExportCallGraph() {
		return this.cmdLine.hasOption(EXPORT_CG.getValue1());
	}
	
	public String getExportCallGraphTxtDestination() {
		return this.cmdLine.getOptionValue(EXPORT_CG_TXT.getValue0());
	}
	
	public String getExportCallGraphBeforeProcessingTxtDestination() {
		return this.cmdLine.getOptionValue(EXPORT_CG_BEFORE_PROCESSING_TXT.getValue0());
	}
	
	public boolean hasExportCallGraphTxt() {
		return this.cmdLine.hasOption(EXPORT_CG_TXT.getValue1());
	}
	
	public boolean hasExportCallBeforeProcessingGraphTxt() {
		return this.cmdLine.hasOption(EXPORT_CG_BEFORE_PROCESSING_TXT.getValue1());
	}
	
	public boolean hasTaintAnalysis() {
		return this.cmdLine.hasOption(TAINT_ANALYSIS.getValue1());
	}
	
	public boolean hasTimeout() {
		return this.cmdLine.hasOption(TIMEOUT.getValue1());
	}
	
	public int getTimeout() {
		return Integer.parseInt(this.cmdLine.getOptionValue(TIMEOUT.getValue0()));
	}
	
	public boolean hasRaw() {
		return this.cmdLine.hasOption(RAW.getValue1());
	}
}
