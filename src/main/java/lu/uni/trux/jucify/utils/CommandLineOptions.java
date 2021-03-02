package lu.uni.trux.jucify.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

	private static final Triplet<String, String, String> APK = new Triplet<String, String, String>("apk", "a", "Apk file");
	private static final Triplet<String, String, String> HELP = new Triplet<String, String, String>("help", "h", "Print this message");
	private static final Triplet<String, String, String> DOT = new Triplet<String, String, String>("dot", "d", "Specify the dot file to load");
	private static final Triplet<String, String, String> EXPORT_CG = new Triplet<String, String, String>("export-cg-to-dot", "e", "Export call graph to dot file");
	private static final Triplet<String, String, String> TIMEOUT = new Triplet<String, String, String>("timeout", "t", "Set the timeout for analysis");
	private static final Triplet<String, String, String> PLATFORMS =
			new Triplet<String, String, String>("platforms", "p", "Android platforms folder");

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
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Initialization of all recognized options
	 */
	private void initOptions() {
		final Option apk = Option.builder(APK.getValue1())
				.longOpt(APK.getValue0())
				.desc(APK.getValue2())
				.hasArg(true)
				.argName(APK.getValue0())
				.required(true)
				.build();
		
		final Option dot = Option.builder(DOT.getValue1())
				.longOpt(DOT.getValue0())
				.desc(DOT.getValue2())
				.hasArg(true)
				.argName(DOT.getValue0())
				.required(true)
				.build();
		
		final Option to = Option.builder(TIMEOUT.getValue1())
				.longOpt(TIMEOUT.getValue0())
				.desc(TIMEOUT.getValue2())
				.hasArg(true)
				.argName(TIMEOUT.getValue0())
				.required(false)
				.build();
		
		final Option export = Option.builder(EXPORT_CG.getValue1())
				.longOpt(EXPORT_CG.getValue0())
				.desc(EXPORT_CG.getValue2())
				.hasArg(true)
				.argName(EXPORT_CG.getValue0())
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

		this.options.addOption(apk);
		this.options.addOption(platforms);
		this.options.addOption(to);
		this.options.addOption(dot);
		this.options.addOption(export);

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
	
	public String getDot() {
		return this.cmdLine.getOptionValue(DOT.getValue0());
	}
	
	public String getExportCallGraphDestination() {
		return this.cmdLine.getOptionValue(EXPORT_CG.getValue0());
	}
	
	public boolean hasExportCallGraph() {
		return this.cmdLine.hasOption(EXPORT_CG.getValue1());
	}
	
	public boolean hasTimeout() {
		return this.cmdLine.hasOption(TIMEOUT.getValue1());
	}
	
	public int getTimeout() {
		return Integer.parseInt(this.cmdLine.getOptionValue(TIMEOUT.getValue0()));
	}
}
