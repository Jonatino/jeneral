/**
 * 
 */
package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class InstantiationParams implements Comparable<InstantiationParams> {
	public String templateQualifiedName;
	public File templateFile;
	public Map<String, Object> templateParameters =  new HashMap<String, Object>();
	public String overriddenSimpleName;
	
	public InstantiationParams(String qualifiedName, File templateFile) {
		this.templateQualifiedName = qualifiedName;
		this.templateFile = templateFile;
	}
	public int compareTo(InstantiationParams other) {
		return toString().compareTo(other.toString());
	}
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof InstantiationParams))
			return false;
		return compareTo((InstantiationParams)o) == 0;
	}
	@Override
	public String toString() {
		return templateQualifiedName + " " + templateParameters + " " + overriddenSimpleName;
	}
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}