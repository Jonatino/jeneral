package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import spoon.processing.Builder;
import spoon.reflect.Factory;
import spoon.support.DefaultCoreFactory;
import spoon.support.RuntimeProcessingManager;
import spoon.support.StandardEnvironment;

import com.ochafik.lang.jeneral.examples.ElementsBuilder;
import com.ochafik.lang.jeneral.examples.EvalExample;
import com.ochafik.lang.jeneral.examples.PArrayList;
import com.ochafik.util.listenable.Pair;

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
	
	public static class InstantiationResult implements Comparable<InstantiationResult> {
		final InstantiationParams instantiationParams;
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
	public static Set<InstantiationResult> instantiate(Set<InstantiationParams> params, Set<String> qualifiedTemplateClassNames) throws Exception {
		Factory factory = new Factory(new DefaultCoreFactory(), new StandardEnvironment());
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
	
	/*public static File instantiate(TemplateEngineArgs args, File outputPath) {
		Filer filer = null;
		filer.
	}*/
	public static void main(String[] args) {
		System.out.println("Entered main");
		//SpoonClassLoader loader = new SpoonClassLoader();
		
		try {
			File sourcePath = new File("/Users/ochafik/Prog/Java/sources");
			//File aptSourcePath = new File("/Users/ochafik/Prog/Java/sources/.apt_generated");
			
			//String className = ElementsBuilder.class.getName();
			//String className = PArrayList.class.getName();
			String className = EvalExample.class.getName();
			InstantiationParams params = new InstantiationParams(className);
			params.templateFile = getSourceFile(className, sourcePath);
			params.templateParameters.add(new Pair<String, Object>(null, Integer.TYPE));
			//params.templateParameters.put("E", Integer.TYPE);
			params.templateFile = getSourceFile(className, sourcePath);
			params.templateQualifiedName = className;
			//params.templateParameters.put("T", JLabel.class);
			
			
			Set<InstantiationResult> results = instantiate(Collections.singleton(params), Collections.EMPTY_SET);
			for (InstantiationResult result : results) {
				System.out.println("RESULT for " + result.qualifiedName);
				System.out.println(result.sourceCode);
				System.out.println();
				System.out.println(result.cascadedInstantiations);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static File getSourceFile(String className, File sourcePath) {
		return new File(sourcePath, className.replace('.', '/') + ".java");
	}
}
