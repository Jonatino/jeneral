package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.util.Set;

import spoon.processing.Builder;
import spoon.processing.Environment;
import spoon.reflect.Factory;
import spoon.support.DefaultCoreFactory;
import spoon.support.RuntimeProcessingManager;
import spoon.support.StandardEnvironment;
import spoon.support.builder.JDTCompiler;


public class InstantiationUtils {
	public static String getTemplateInterfaceQualifiedName(String qualName) {
		int i = qualName.lastIndexOf('.');
		if (i < 0)
			return "_" + qualName;
		return qualName.substring(0, i + 1) + "_" + qualName.substring(i + 1);
	}
	
	static void time(String name, Runnable runnable) {
		long start = System.nanoTime();
		runnable.run();
		long time = System.nanoTime() - start;
		System.out.println("[" + name + "] " + (time / 1000000L) + " ms");
	}
	
	public static Set<InstantiationResult> instantiate(Set<InstantiationParams> params) throws Exception {
		Factory factory = new Factory(new DefaultCoreFactory(), new StandardEnvironment()) {
			Environment environment;
			@Override
			public Environment getEnvironment() {
				if (environment == null) {
					environment = new StandardEnvironment() {
						@Override
						public String getSourcePath() {
							if (false)
								return "/Users/ochafik/Prog/Java/sources:/Users/ochafik/Prog/Java/sources/.apt_generated";
							
							String workspacePath = System.getProperty("workspace.projectclasspath");
							if (workspacePath != null)
								return workspacePath;
							
							String classPath = System.getProperty("java.class.path");
							return classPath == null ? "." : classPath;
							//return "/Users/ochafik/Prog/Java/sources:/Users/ochafik/Prog/Java/sources/.apt_generated";
						}
					};
					System.out.println(environment.getSourcePath());
				}
				return environment;
			}
			JDTCompiler c;
		};
		Builder builder = factory.getBuilder();
		for (InstantiationParams param : params)
			builder.addInputSource(param.templateFile);
		
		builder.build();
		
		TemplateInstantiator templateProcessor = new TemplateInstantiator(params);

		RuntimeProcessingManager manager = new RuntimeProcessingManager(factory);
		manager.addProcessor(templateProcessor);
		manager.process();
		
		return templateProcessor.getResults();
	}
	
	static File getSourceFile(String className, File sourcePath) {
		return new File(sourcePath, className.replace('.', '/') + ".java");
	}
}
