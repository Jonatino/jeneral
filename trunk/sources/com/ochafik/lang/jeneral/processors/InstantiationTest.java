package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import com.ochafik.lang.jeneral.examples.EvalExample;
import com.ochafik.util.listenable.Pair;

public class InstantiationTest {

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
			//String className = Testt.class.getName();
			InstantiationParams params = new InstantiationParams(className);
			params.templateFile = InstantiationUtils.getSourceFile(className, sourcePath);
			params.templateParameters.add(new Pair<String, Object>(null, Integer.TYPE));
			//params.templateParameters.put("E", Integer.TYPE);
			params.templateFile = InstantiationUtils.getSourceFile(className, sourcePath);
			params.templateQualifiedName = className;
			//params.templateParameters.put("T", JLabel.class);
			
			
			Set<InstantiationResult> results = InstantiationUtils.instantiate(Collections.singleton(params), Collections.EMPTY_SET);
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

}
