package com.ochafik.lang.jeneral.examples;

import java.io.File;

import com.sun.tools.apt.Main;

public class AptTest {
	public static void main(String[] args) {
		System.out.println("Running in " + new File(".").getAbsolutePath());
		String cmd;
		
		if (false) {
			cmd = 
				"-cp classes:libraries/velocity.jar:libraries/spoon.jar " +
				"-factory com.ochafik.lang.jeneral.processors.TemplateProcessorFactory " +
				"-d classes " +
				"-s sources/.apt_generated " +
				//"sources/com/ochafik/lang/jeneral/examples/ElementsBuilder.java " +
				"sources/com/ochafik/lang/jeneral/examples/InstantiationExample.java"
			;
		} else {
			cmd = 
				"-cp bin/jeneral.jar:libraries/velocity.jar:libraries/spoon.jar " +
				"-factorypath bin/jeneral.jar " +
				"-d classes " +
				"-s sources/.apt_generated " +
				//"sources/com/ochafik/lang/jeneral/examples/ElementsBuilder.java " +
				"sources/com/ochafik/lang/jeneral/examples/InstantiationExample.java"
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
