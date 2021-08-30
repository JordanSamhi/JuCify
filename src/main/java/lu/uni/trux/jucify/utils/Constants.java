package lu.uni.trux.jucify.utils;

import java.io.File;

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

public class Constants {

	/**
	 * Misc
	 */
	public static final String JUCIFY = "JuCify";
	public static final String NODE_PREFIX = "Node_";
	public static final String INIT = "<init>";
	public static final String GRAPH_NAME = "Graph Name";
	public static final String COMMENT_ENTRYPOINTS_FILE = "#";
	public static final int JAVA_INVOKEE = 0;
	public static final int RETURN_VALUE = 1;
	public static final String SOURCE = "SOURCE";
	public static final String SINK = "SINK";
	public static final String VOID = "void";
	public static final String VERSION = "0.1";
	public static final String OPAQUE_PREDICATE_LOCAL = "opaque_predicate_local";
	public static final String TARGET_TMP_DIR = String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, "jucify");
	
	/**
	 * Classes
	 */
	public static final String DUMMY_BINARY_CLASS = "DummyBinaryClass";
	public static final String JAVA_LANG_OBJECT = "java.lang.Object";
	
	/**
	 * Methods
	 */
	public static final String INIT_METHOD_SUBSIG = "void <init>()";
	
	/**
	 * Files
	 */
	public static final String SOURCES_SINKS_FILE = "/SourcesSinks.txt";
	public static final String EASY_TAINT_WRAPPER_FILE = "/EasyTaintWrapperSource.txt";
}
