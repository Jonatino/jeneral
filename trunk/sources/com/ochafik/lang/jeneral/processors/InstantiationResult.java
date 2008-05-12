/**
 * 
 */
package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

public class InstantiationResult implements Comparable<InstantiationResult> {
	static class SourcePos {
		File file;
		int column, row;
	}
	final InstantiationParams instantiationParams;
	SourcePos instantiationPos;
	
	String qualifiedName;
	String sourceCode;
	Set<InstantiationParams> cascadedInstantiations = new TreeSet<InstantiationParams>();
	public InstantiationResult(InstantiationParams instantiationParams) {
		this.instantiationParams = instantiationParams;
	}
	public int compareTo(InstantiationResult o) {
		return qualifiedName.compareTo(o.qualifiedName);
	}
	@Override
	public int hashCode() {
		return qualifiedName.hashCode();
	}
}