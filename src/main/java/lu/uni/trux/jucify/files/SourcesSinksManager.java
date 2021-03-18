package lu.uni.trux.jucify.files;

import java.util.HashSet;
import java.util.Set;

import lu.uni.trux.jucify.utils.Constants;
import lu.uni.trux.jucify.utils.Utils;
import soot.SootMethod;
import soot.jimple.infoflow.android.data.AndroidMethod;

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

public class SourcesSinksManager extends FileLoader {

	private static SourcesSinksManager instance;
	private Set<AndroidMethod> sources;
	private Set<AndroidMethod> sinks;

	private SourcesSinksManager () {
		super();
		this.sources = new HashSet<AndroidMethod>();
		this.sinks = new HashSet<AndroidMethod>();
		this.loadSourcesSinks();
	}

	public static SourcesSinksManager v() {
		if(instance == null) {
			instance = new SourcesSinksManager();
		}
		return instance;
	}

	private void loadSourcesSinks() {
		String[] split = null;
		String type = null,
				signature = null;
		for(String method: this.items) {
			split = method.split("\\|");
			if(split.length == 2) {
				type = split[0];
				signature = split[1];
				AndroidMethod am = new AndroidMethod(Utils.getMethodNameFromSignature(signature),
						Utils.getParametersNamesFromSignature(signature),
						Utils.getReturnNameFromSignature(signature),
						Utils.getClassNameFromSignature(signature));
				if(type.equals(Constants.SOURCE)) {
					this.sources.add(am);
				}else if(type.equals(Constants.SINK)) {
					this.sinks.add(am);
				}
			}
		}
	}

	public Set<AndroidMethod> getSources() {
		return this.sources;
	}

	public Set<AndroidMethod> getSinks() {
		return this.sinks;
	}

	public void addSink(SootMethod sm) {
		this.sinks.add(new AndroidMethod(sm));
	}
	
	public void addSource(SootMethod sm) {
		this.sources.add(new AndroidMethod(sm));
	}

	@Override
	protected String getFile() {
		return Constants.SOURCES_SINKS_FILE;
	}
}