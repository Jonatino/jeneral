package com.ochafik.lang.jeneral.scripting;

import java.io.File;

public interface ScriptingManager {
	public String eval(String program, File path, String language);
	public void define(String name, Object object);
	
	public static class Factory {
		private static ScriptingManager manager; 
		public static ScriptingManager getInstance() {
			if (manager == null) {
				try {
					// Try Java 6's Scripting API
					Class.forName("javax.script.ScriptEngineManager");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				
				try {
					// Try Apache's Bean Scripting Framework
					Class.forName("org.apache.bsf.BSFManager");
					manager = ScriptingManager.class.cast(Class.forName("com.ochafik.lang.jeneral.scripting.BSFScriptingManager").newInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return manager;
		}
	}
}
