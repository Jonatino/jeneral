package com.ochafik.lang.jeneral.scripting;

import java.io.File;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

public class BSFScriptingManager implements ScriptingManager {
	BSFManager manager = new BSFManager();

	public void define(String name, Object object) {
		try {
			manager.declareBean(name, object, object.getClass());
		} catch (BSFException e) {
			e.printStackTrace();
		}	
	}

	public String eval(String program, File path, String language) {
		return null;//return manager.eval(language, source, line, column, program);
	}
}
