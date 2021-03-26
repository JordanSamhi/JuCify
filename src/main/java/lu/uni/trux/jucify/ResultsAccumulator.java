package lu.uni.trux.jucify;

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

public class ResultsAccumulator {

	private static ResultsAccumulator instance;

	private String appName;
	private long analysisElapsedTime;
	private long taintAnalysisElapsedTime;
	private long instrumentationElapsedTime;
	private int numberNewCallGraphNodes;
	private int numberNewJavaToNativeCallGraphEdges;
	private int numberNewNativeToJavaCallGraphEdges;
	private int numberNewCallGraphReachableNodes;
	private boolean hasFlowThroughNative;

	private ResultsAccumulator () {
		this.setAppName("");
		this.setAnalysisElapsedTime(0);
		this.setInstrumentationElapsedTime(0);
		this.setTaintAnalysisElapsedTime(0);
		this.setNumberNewCallGraphNodes(0);
		this.setNumberNewJavaToNativeCallGraphEdges(0);
		this.setNumberNewNativeToJavaCallGraphEdges(0);
		this.setNumberNewCallGraphReachableNodes(0);
		this.setHasFlowThroughNative(false);
	}

	public static ResultsAccumulator v() {
		if(instance == null) {
			instance = new ResultsAccumulator();
		}
		return instance;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void printVectorResults() {
		System.out.println(this.generateVector());
	}

	private String generateVector() {
		return String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d", this.getAppName(), this.getAnalysisElapsedTime(),
				this.getInstrumentationElapsedTime(), this.getTaintAnalysisElapsedTime(),
				this.getNumberNewCallGraphNodes(), this.getNumberNewJavaToNativeCallGraphEdges(),
				this.getNumberNewNativeToJavaCallGraphEdges(), this.getNumberNewCallGraphReachableNodes(),
				this.hasFlowThroughNative ? 1 : 0);
	}

	public long getAnalysisElapsedTime() {
		return analysisElapsedTime;
	}

	public void setAnalysisElapsedTime(long l) {
		this.analysisElapsedTime = l;
	}

	public long getTaintAnalysisElapsedTime() {
		return taintAnalysisElapsedTime;
	}

	public void setTaintAnalysisElapsedTime(long taintAnalysisElapsedTime) {
		this.taintAnalysisElapsedTime = taintAnalysisElapsedTime;
	}

	public long getInstrumentationElapsedTime() {
		return instrumentationElapsedTime;
	}

	public void setInstrumentationElapsedTime(long instrumentationElapsedTime) {
		this.instrumentationElapsedTime = instrumentationElapsedTime;
	}

	public int getNumberNewCallGraphNodes() {
		return numberNewCallGraphNodes;
	}

	public void setNumberNewCallGraphNodes(int numberNewCallGraphNodes) {
		this.numberNewCallGraphNodes = numberNewCallGraphNodes;
	}

	public int getNumberNewJavaToNativeCallGraphEdges() {
		return numberNewJavaToNativeCallGraphEdges;
	}

	public void setNumberNewJavaToNativeCallGraphEdges(int numberNewJavaToNativeCallGraphEdges) {
		this.numberNewJavaToNativeCallGraphEdges = numberNewJavaToNativeCallGraphEdges;
	}

	public int getNumberNewNativeToJavaCallGraphEdges() {
		return numberNewNativeToJavaCallGraphEdges;
	}

	public void setNumberNewNativeToJavaCallGraphEdges(int numberNewNativeToJavaCallGraphEdges) {
		this.numberNewNativeToJavaCallGraphEdges = numberNewNativeToJavaCallGraphEdges;
	}

	public void incrementNumberNewJavaToNativeCallGraphEdges() {
		this.setNumberNewJavaToNativeCallGraphEdges(this.getNumberNewJavaToNativeCallGraphEdges() + 1);
	}

	public void incrementNumberNewCallGraphNodes() {
		this.setNumberNewCallGraphNodes(this.getNumberNewCallGraphNodes() + 1);
	}

	public void incrementNumberNewNativeToJavaCallGraphEdges() {
		this.setNumberNewNativeToJavaCallGraphEdges(this.getNumberNewNativeToJavaCallGraphEdges() + 1);
	}

	public int getNumberNewCallGraphReachableNodes() {
		return numberNewCallGraphReachableNodes;
	}

	public void setNumberNewCallGraphReachableNodes(int numberNewCallGraphReachableNodes) {
		this.numberNewCallGraphReachableNodes = numberNewCallGraphReachableNodes;
	}

	public boolean isHasFlowThroughNative() {
		return hasFlowThroughNative;
	}

	public void setHasFlowThroughNative(boolean hasFlowThroughNative) {
		this.hasFlowThroughNative = hasFlowThroughNative;
	}
}