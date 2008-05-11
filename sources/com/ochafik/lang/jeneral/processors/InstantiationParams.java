/**
 * 
 */
package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ochafik.util.listenable.Pair;

public class InstantiationParams implements Comparable<InstantiationParams> {
	public String templateQualifiedName;
	public File templateFile;
	List<Pair<String, Object>> templateParameters  = new ArrayList<Pair<String, Object>>();
	//public Map<String, Object> templateParameters =  new HashMap<String, Object>();
	public String overriddenSimpleName;
	
	public InstantiationParams(String qualifiedName) {
		this.templateQualifiedName = qualifiedName;
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