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
	private int numberNewJavaToNativeCallGraphEdges;
	private int numberNewNativeToJavaCallGraphEdges;
	private int numberNewCallGraphReachableNodesJava;
	private int numberNewCallGraphReachableNodesNative;
	private int numberNewCallGraphReachableNodes;
	private int numberNewEdges;
	private int numberNodesBeforeJucify;
	private int numberNodesAfterJucify;
	private int numberEdgesBeforeJucify;
	private int numberEdgesAfterJucify;
	private boolean hasFlowThroughNative;

	private ResultsAccumulator () {
		this.setAppName("");
		this.setAnalysisElapsedTime(0);
		this.setInstrumentationElapsedTime(0);
		this.setTaintAnalysisElapsedTime(0);
		this.setNumberNewJavaToNativeCallGraphEdges(0);
		this.setNumberNewNativeToJavaCallGraphEdges(0);
		this.setNumberNewCallGraphReachableNodes(0);
		this.setNumberNodesAfterJucify(0);
		this.setNumberNodesBeforeJucify(0);
		this.setNumberEdgesAfterJucify(0);
		this.setNumberEdgesBeforeJucify(0);
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
		return String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", this.getAppName(), this.getAnalysisElapsedTime(),
				this.getInstrumentationElapsedTime(), this.getTaintAnalysisElapsedTime(),
				this.getNumberNewJavaToNativeCallGraphEdges(), this.getNumberNewNativeToJavaCallGraphEdges(),
				this.getNumberNewCallGraphReachableNodes(), this.getNumberNewCallGraphReachableNodesJava(),
				this.getNumberNewCallGraphReachableNodesNative(), this.getNumberNewEdges(),
				this.hasFlowThroughNative ? 1 : 0, this.getNumberNodesBeforeJucify(), this.getNumberNodesAfterJucify(),
						this.getNumberEdgesBeforeJucify(), this.getNumberEdgesAfterJucify());
	}
	
	public void printResults() {
		System.out.println("Results:");
		System.out.println(String.format(" - App name: %s", this.getAppName()));
		System.out.println(String.format(" - Analysis elapsed time: %d", this.getAnalysisElapsedTime()));
		System.out.println(String.format(" - Instrumentation elapsed time: %d", this.getInstrumentationElapsedTime()));
		System.out.println(String.format(" - Taint Analysis elapsed time: %d", this.getTaintAnalysisElapsedTime()));
		System.out.println(String.format(" - Number of nodes before Jucify: %d", this.getNumberNodesBeforeJucify()));
		System.out.println(String.format(" - Number of nodes after Jucify: %d", this.getNumberNodesAfterJucify()));
		System.out.println(String.format(" - Number of edges before Jucify: %d", this.getNumberEdgesBeforeJucify()));
		System.out.println(String.format(" - Number of edges after Jucify: %d", this.getNumberEdgesAfterJucify()));
		System.out.println(String.format(" - Number new Java-to-Native Call-Graph edges: %d", this.getNumberNewJavaToNativeCallGraphEdges()));
		System.out.println(String.format(" - Number new Native-to-Java Call-Graph edges: %d", this.getNumberNewNativeToJavaCallGraphEdges()));
		System.out.println(String.format(" - Number new Call-Graph reachable nodes: %d", this.getNumberNewCallGraphReachableNodes()));
		System.out.println(String.format(" - Number new Call-Graph reachable nodes Java: %d", this.getNumberNewCallGraphReachableNodesJava()));
		System.out.println(String.format(" - Number new Call-Graph reachable nodes Native: %d", this.getNumberNewCallGraphReachableNodesNative()));
		System.out.println(String.format(" - Number new Call-Graph edges: %d", this.getNumberNewEdges()));
		System.out.println(String.format(" - Has flow through native: %s", this.hasFlowThroughNative ? "Yes" : "No"));
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

	public int getNumberNewCallGraphReachableNodesJava() {
		return numberNewCallGraphReachableNodesJava;
	}

	public void setNumberNewCallGraphReachableNodesJava(int numberNewCallGraphReachableNodesJava) {
		this.numberNewCallGraphReachableNodesJava = numberNewCallGraphReachableNodesJava;
	}

	public int getNumberNewCallGraphReachableNodesNative() {
		return numberNewCallGraphReachableNodesNative;
	}

	public void setNumberNewCallGraphReachableNodesNative(int numberNewCallGraphReachableNodesNative) {
		this.numberNewCallGraphReachableNodesNative = numberNewCallGraphReachableNodesNative;
	}

	public int getNumberNewEdges() {
		return numberNewEdges;
	}

	public void setNumberNewEdges(int numberNewEdges) {
		this.numberNewEdges = numberNewEdges;
	}

	public int getNumberNodesBeforeJucify() {
		return numberNodesBeforeJucify;
	}

	public void setNumberNodesBeforeJucify(int numberNodesBeforeJucify) {
		this.numberNodesBeforeJucify = numberNodesBeforeJucify;
	}

	public int getNumberNodesAfterJucify() {
		return numberNodesAfterJucify;
	}

	public void setNumberNodesAfterJucify(int numberNodesAfterJucify) {
		this.numberNodesAfterJucify = numberNodesAfterJucify;
	}

	public int getNumberEdgesBeforeJucify() {
		return numberEdgesBeforeJucify;
	}

	public void setNumberEdgesBeforeJucify(int numberEdgesBeforeJucify) {
		this.numberEdgesBeforeJucify = numberEdgesBeforeJucify;
	}

	public int getNumberEdgesAfterJucify() {
		return numberEdgesAfterJucify;
	}

	public void setNumberEdgesAfterJucify(int numberEdgesAfterJucify) {
		this.numberEdgesAfterJucify = numberEdgesAfterJucify;
	}
}