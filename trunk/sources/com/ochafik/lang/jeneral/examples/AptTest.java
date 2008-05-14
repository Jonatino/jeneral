package com.ochafik.lang.jeneral.examples;

import java.io.File;

import com.sun.tools.apt.Main;

public class AptTest {
	static String source(Class<?> c) {
		return sourcePath + c.getName().replace('.', '/') + ".java "; 
	}
	static String sourcePath = "";//"sources/";
	
	public static void main(String[] args) {
		System.out.println("Running in " + new File(".").getAbsolutePath());
		String cmd;
		if (true) {
			cmd = 
				"-cp ../classes:../libraries/velocity.jar:../libraries/spoon.jar " +
				"-factory com.ochafik.lang.jeneral.processors.TemplateProcessorFactory " +
				"-d ../classes " +
				"-s ../sources/.apt_generated " +
				source(ElementsBuilder.class) +
				source(EvalExample.class) +
				source(InstantiationExample.class) + 
				source(EvalExample.class) + 
				source(PArrayList.class)
			;
		} else {
			cmd = 
				"-cp bin/jeneral.jar:libraries/velocity.jar:libraries/spoon.jar " +
				"-factorypath bin/jeneral.jar " +
				"-d classes " +
				"-s sources/.apt_generated " +
				source(ElementsBuilder.class) +
				source(InstantiationExample.class)
			;
		}
		//ElementsBuilder__Integer
		try {
			Main.main(cmd.split(" "));
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}
}
