/*
   Copyright 2008 Olivier Chafik

   Licensed under the Apache License, Version 2.0 (the License);
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an AS IS BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This file comes from the Jeneral project (Java Reifiable Generics & Class Templates)

       http://jeneral.googlecode.com/.
*/
package com.ochafik.lang.jeneral.processors;

import static com.ochafik.lang.SyntaxUtils.array;
import static com.ochafik.util.string.StringUtils.implode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

import sun.tools.javap.oldjavap.JavaP;

import com.ochafik.admin.JavaParsingUtils;
import com.ochafik.admin.velocity.IOTool;
import com.ochafik.admin.velocity.MapWrap;
import com.ochafik.admin.velocity.RuntimeTool;
import com.ochafik.io.ReadText;
import com.ochafik.lang.jeneral.AbstractTemplateClass;
import com.ochafik.lang.jeneral.Array;
import com.ochafik.lang.jeneral.ReificationUtils;
import com.ochafik.lang.jeneral.TemplateClass;
import com.ochafik.lang.jeneral.TemplateContractViolationException;
import com.ochafik.lang.jeneral.TemplateInstance;
import com.ochafik.lang.jeneral.annotations.Include;
import com.ochafik.lang.jeneral.annotations.Includes;
import com.ochafik.lang.jeneral.annotations.InlineVelocity;
import com.ochafik.lang.jeneral.annotations.Param;
import com.ochafik.lang.jeneral.annotations.ParamConstructor;
import com.ochafik.lang.jeneral.annotations.Instantiate;
import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.annotations.TemplatesHelper;
import com.ochafik.lang.jeneral.annotations.Value;
import com.ochafik.lang.jeneral.processors.AbstractProcessor.LinesFormatter;
import com.ochafik.util.CompoundCollection;
import com.ochafik.util.listenable.Pair;
import com.ochafik.util.string.RegexUtils;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.MirroredTypeException;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.SourcePosition;

/*
cd /Users/ochafik/Prog/Java && rm templates_logs.txt >/dev/null ; apt -factory com.ochafik.lang.templates.TemplateProcessorFactory -d classes/ -s sources/.apt_generated/ -cp sources:classes sources/com/ochafik/lang/templates/*.java && open templates_logs.txt
 */
public class TemplateProcessor extends AbstractProcessor {
	private static final String 
		//GENERATED_INTERFACE _SUFFIX = "_Template",
		GENERATED_FACTORY_NAME = "template"/*,
		GENERATED_IMPLEMENTATION_NAME = "_"*/;
		
	@SuppressWarnings({ "unchecked", "unused" })
	private Class<?>[] deps = array(
		ReificationUtils.class,
		TemplateInstance.class,
		TemplatesHelper.class,
		InlineVelocity.class,
		Includes.class,
		Include.class,
		Instantiate.class,
		Param.class,
		ParamConstructor.class,
		Property.class,
		Template.class,
		Value.class
	);
 
	public TemplateProcessor(AnnotationProcessorEnvironment env){
		super(env);

		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new LogChute() {
			public void init(RuntimeServices arg0) throws Exception {}
			public boolean isLevelEnabled(int level) {
				return level == ERROR_ID;
			}
			public void log(int arg0, String arg1) {
				if (arg0 == ERROR_ID)
					TemplateProcessor.this.log(ERROR_PREFIX + arg1);
			}
			public void log(int arg0, String arg1, Throwable arg2) {}
			
		});
		try {
			Velocity.init();
		} catch (Exception e) {
			log("On Velocity.init() : " + e);
		}
		
		context = new VelocityContext();
		context.put("context", context);
		//context.put("lists", new ListTool());
		//context.put("alternator", new AlternatorTool());
		//context.put("date", new ComparisonDateTool());
		//context.put("escape", new EscapeTool());
		//context.put("math", new MathTool());
		//context.put("number", new NumberTool());
		//context.put("sort", new SortTool());
		//context.put("resource", new ResourceTool());
		//context.put("runtime", new RuntimeTool());
		context.put("io", new IOTool());
		context.put("environment", environment);
		//context.put("environment", new MapWrap<String,String>(System.getenv()));
		//context.put("properties", new SystemProperties());
		//context.put("arguments", applicationArgs);
	}
	
	Context context;

	public class GeneratedMethodInfo {
		String modifiers, returnType, throwsClause, name, argumentsDeclaration;
		StringWriter body = new StringWriter();
		PrintWriter pbody = new PrintWriter(body);
		boolean inHeader, inImplementation;
	}
	public class TemplateInfo {
		final ClassDeclaration classDeclaration;
		final String templateInterfaceQualifiedName;
		final String templateInterfaceName;
		final String packageName;
		final String genericParamsDefinition;
		
		final List<String> genericParamNames;
		final String genericParamsUsage;
		
		final Map<String, Pair<MethodDeclaration, List<MethodDeclaration>>> paramConstructorContracts;
		final List<FieldDeclaration> properties;
		final List<FieldDeclaration> veloScripts;
		final List<FieldDeclaration> propertiesToAddToConstructors;
		final String qualifiedTemplateNameWithGenericsUsage;
		final String implemClassName;
		
		final List<String> additionalHeaders = new ArrayList<String>();
		final List<String> additionalImports = new ArrayList<String>();
		final List<String> additionalImplemMembers = new ArrayList<String>();
		
		//final boolean implemClassIsAbstract;
		final List<GeneratedMethodInfo> generatedMethods = new ArrayList<GeneratedMethodInfo>();
		
		public GeneratedMethodInfo newMethod(String name, Class<?>[] arguments, Class<? extends Throwable>[] throwedTypes) {
			GeneratedMethodInfo m = new GeneratedMethodInfo();
			m.name = name;
			//m.argumentsDeclaration =
			generatedMethods.add(m);
			return m;
		}
		
		public TemplateInfo(ClassDeclaration classDeclaration) {
			this.classDeclaration = classDeclaration;
			//templateInterfaceQualifiedName = classDeclaration.getQualifiedName() + GENERATED_INTERFACE_SUFFIX;
			//templateInterfaceName = classDeclaration.getSimpleName() + GENERATED_INTERFACE_SUFFIX;
			
			templateInterfaceQualifiedName = RegexUtils.regexReplace(Pattern.compile("^((?:.*\\.)?)?(\\w+)$"), classDeclaration.getQualifiedName(), new MessageFormat("{1}_{2}"));
			templateInterfaceName = "_" + classDeclaration.getSimpleName();
			implemClassName = classDeclaration.getSimpleName() + "Impl";
			
			packageName = classDeclaration.getPackage().getQualifiedName();
			List<String> genDefs = new ArrayList<String>();
			for (TypeParameterDeclaration d : classDeclaration.getFormalTypeParameters()) {
				genDefs.add(d.getSimpleName() + wrappedImplosion(d.getBounds(), " extends ", ""));
			}
			genericParamsDefinition = wrappedImplosion(genDefs, "<", ">");
			
			genericParamNames = new ArrayList<String>(getFormalTypeNames(classDeclaration));
			genericParamsUsage = wrappedImplosion(genericParamNames, "<", ">");
			
			paramConstructorContracts = getGenericParamConstructorsContracts(classDeclaration, genericParamNames);
			veloScripts = new ArrayList<FieldDeclaration>();
			properties = new ArrayList<FieldDeclaration>();
			propertiesToAddToConstructors = new ArrayList<FieldDeclaration>();
			for (FieldDeclaration field : classDeclaration.getFields()) {
				Property prop = field.getAnnotation(Property.class);
				InlineVelocity velo = field.getAnnotation(InlineVelocity.class);
				if (prop == null && velo == null)
					continue;
				
				if (prop != null) {
					if (field.getModifiers().contains(Modifier.PRIVATE))
						printError(field, "Properties cannot be private.");
						
					properties.add(field);
					
					if (prop.construct()) {
						if (field.getModifiers().contains(Modifier.FINAL))
							printError(field, "Cannot define a final field in any of this template's implementation constructors, as final fields must be initialized by a constructor of this class.");
						else 
							propertiesToAddToConstructors.add(field);
					}
				}
				
				if (velo != null) {
					if (field.getModifiers().contains(Modifier.PRIVATE))
						printError(field, "Inline scripts must be attached to non-private uninitialized fields.");
						
					veloScripts.add(field);
				}
			}
			
			//implemClassIsAbstract = !(genericParamNames.isEmpty() && propertiesToAddToConstructors.isEmpty());
			qualifiedTemplateNameWithGenericsUsage = classDeclaration.getQualifiedName() + genericParamsUsage;
		}
	};
	
	public class ConstructorInfo {
		final ConstructorDeclaration constructorDeclaration;
		final List<String> originalCtorArgNames;
		final Set<String> existingArgumentNames;
		final Map<String, String> genericParamClassArgNames;
		final Map<String, String> propertiesInitArgNames;
		final String arraySizeArgName;
		final List<String> generatedConstructorArgumentsDeclarations;
		final List<String> generatedConstructorArguments;
		final List<String> generatedFactoryArgumentsDeclarations;
		final List<String> generatedFactoryArguments;
		public ConstructorInfo(TemplateInfo templateClassInfo, ConstructorDeclaration constructorDeclaration) {
			this.constructorDeclaration = constructorDeclaration;
			originalCtorArgNames = getArgumentNames(constructorDeclaration, null);
			existingArgumentNames = new TreeSet<String>(originalCtorArgNames);
			genericParamClassArgNames = new TreeMap<String, String>();
			propertiesInitArgNames = new TreeMap<String, String>();
			
			for (String genericParamName : templateClassInfo.genericParamNames)
				genericParamClassArgNames.put(genericParamName, chooseUniqueName(decapitalize(genericParamName), existingArgumentNames, true)); 
			for (FieldDeclaration propertyDecl : templateClassInfo.propertiesToAddToConstructors)
				propertiesInitArgNames.put(propertyDecl.getSimpleName(), chooseUniqueName(decapitalize(propertyDecl.getSimpleName()), existingArgumentNames, true));
			
			arraySizeArgName = chooseUniqueName("arraySize", existingArgumentNames, false);
			
			// Create the list of arguments declarations
			generatedFactoryArgumentsDeclarations = new ArrayList<String>();
			generatedFactoryArguments = new ArrayList<String>();
			for (String genericParamName : templateClassInfo.genericParamNames) {
				String name = genericParamClassArgNames.get(genericParamName);
				generatedFactoryArgumentsDeclarations.add("final " + typedClass(genericParamName) + " " + name);
				generatedFactoryArguments.add(name);
			}
			
			generatedConstructorArgumentsDeclarations = new ArrayList<String>();
			generatedConstructorArguments = new ArrayList<String>();
			for (Declaration d : union(constructorDeclaration.getParameters(), templateClassInfo.propertiesToAddToConstructors)) {
				String type = getType(d);
				if (type == null)
					continue;
				
				String name = d.getSimpleName();
				try {
					TypeDeclaration t = environment.getTypeDeclaration(type);
					type = t.getQualifiedName();
				} catch (Exception ex) {}
				generatedConstructorArgumentsDeclarations.add("final " + type + " " + name);
				generatedConstructorArguments.add(name);
			}
			generatedFactoryArgumentsDeclarations.addAll(generatedConstructorArgumentsDeclarations);
			generatedFactoryArguments.addAll(generatedConstructorArguments);
		}
	}
	static String getType(Declaration d) {
		if (d instanceof ParameterDeclaration) {
			return ((ParameterDeclaration)d).getType().toString();
		}
		if (d instanceof FieldDeclaration) {
			return ((FieldDeclaration)d).getType().toString();
		}
		return null;
	}
	static <E> Collection<? extends E> union(Collection<? extends E>... cols) {
		return new CompoundCollection<E>(Arrays.asList(cols));
	}
	static String computeInstanceName(String baseName, TypeDeclaration... types) {
		StringBuilder b = new StringBuilder(baseName);
		for (TypeDeclaration type : types) {
			b.append("__");
			b.append(type.getSimpleName());
		}
		return b.toString();
	}
	
	void logClass(Class<?> c) {
		log(c.getName());
		for (Method m : c.getDeclaredMethods())
			log("\t" + m);
	}
	void logClassHier(Class<?> c) {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		while (c != null && classes.add(c) && c != Object.class)
		{
			logClass(c);
			for (Class<?> ci : c.getInterfaces()) {
				log(ci.getName());
				for (Method m : ci.getMethods())
					log("\t" + m);
			}
			c = c.getSuperclass();
		}
	}
	public void process() {
		AnnotationTypeDeclaration templateAnno = getAnnotationType(Template.class);
		
		for (Declaration dec : environment.getDeclarationsAnnotatedWith(templateAnno)) {
			//logClassHier(c);
			
			if (!(dec instanceof ClassDeclaration)) {
				printError(dec, "Only classes may be annotated with " + Template.class.getName());
				continue;
			}
			ClassDeclaration decl = (ClassDeclaration)dec;
			try {
				processTemplateClass(decl);
			} catch (Throwable t) {
				logError(dec, t);
			}
		}
		
		AnnotationTypeDeclaration instantiationAnno = getAnnotationType(Instantiate.class);
		for (Declaration dec : environment.getDeclarationsAnnotatedWith(instantiationAnno)) {
			try {
				processInstantiation(dec);
			} catch (Throwable t) {
				logError(dec, t);
			}
		}
	}
	
	private void processIncludes(TemplateInfo templateInfo, SourcePosition includesPosition) {
		Includes includes = templateInfo.classDeclaration.getAnnotation(Includes.class);
		if (includes != null) {
			for (Include include : includes.value()) {
				processInclude(templateInfo, include, includesPosition);
			}
		}
		processInclude(templateInfo, templateInfo.classDeclaration.getAnnotation(Include.class), includesPosition);
	}
	private void processInclude(TemplateInfo templateInfo, Include inclusion, SourcePosition includePosition) {
		//Include inclusion = templateInfo.classDeclaration.getAnnotation(Include.class);
		//AnnotationMirror classIncludeAnnotation = findAnnotation(templateInfo.classDeclaration, Include.class);
		//Object includedClassAnnoValue = classIncludeAnnotation == null ? null : findAnnotationValueOfType(classIncludeAnnotation, Class.class);
		//Object includedClassAnnoValue = classIncludeAnnotation == null ? null : findAnnotationValueForName(classIncludeAnnotation, "type");
		
		/*if (includedClassAnnoValue == null) {
			printError(classIncludeAnnotation == null ? templateInfo.classDeclaration.getPosition() : classIncludeAnnotation.getPosition(), 
					classIncludeAnnotation == null ? "Did not find the annotation !" : 
					"Not enough arguments. Needs a type reference or a script.");
			return;
		}
		*/
		if (inclusion == null)
			return;
		
		String scriptName = inclusion.script();
		if (scriptName.length() > 0) {
//			String script = getDocCommentContent(veloField);
//			if (script == null) {
//				printError(veloField, "Failed to parse the inline velocity script. Make sure it is right before the @" + InlineVelocity.class.getName() + " annotation.");
//				continue;
//			}
			
			try {
				//String script = getDocCommentContent(veloField);
//				if (script == null) {
//					printError(veloField, "Failed to parse the inline velocity script. Make sure it is right before the @" + InlineVelocity.class.getName() + " annotation.");
//					continue;
//				}
				String scriptSource = readResource(scriptName, templateInfo.classDeclaration.getPosition().file(), templateInfo.classDeclaration.getPackage().getQualifiedName());
				String scriptResult = evalScript(templateInfo, scriptName, scriptSource, includePosition);
				
				JavaParsingUtils.SlicedCompilationUnit.SlicedClass slicedScriptClass = JavaParsingUtils.sliceClassBody(scriptResult);
				templateInfo.additionalHeaders.addAll(slicedScriptClass.getInnerClassesAndVisibleInstanceMethodsDeclarations());
				templateInfo.additionalImplemMembers.addAll(slicedScriptClass.getVisibleInstanceMethodsImplementations());
				
			} catch (IOException e) {
				logError(templateInfo.classDeclaration, e);
			}
			
			
		} else {
			String typeName;
			try {
				typeName = inclusion.type().getName();
			} catch (MirroredTypeException ex) {
				typeName = ex.getTypeMirror().toString();
			}
			if (typeName.length() > 0 && !typeName.equals(Object.class.getName())) {
				TypeDeclaration includedType = environment.getTypeDeclaration(typeName);
				if (!(includedType instanceof ClassDeclaration)) {
					printError(templateInfo.classDeclaration, "Only class inclusions are supported (included " + typeName +")");
				} else {
					ClassDeclaration includedClass = (ClassDeclaration)includedType;
					if (includedClass.getDeclaringType() != null) {
						printError(templateInfo.classDeclaration, "Inclusion of inner classes is not supported. Please include a top-level class.");
						return;
					}
					SourcePosition pos = includedClass.getPosition();
					String includedClassSource = ReadText.readText(pos.file());
					
					try {
						JavaParsingUtils.SlicedCompilationUnit includedClassCompilationUnit = JavaParsingUtils.sliceCompilationUnitSource(includedClassSource);
						JavaParsingUtils.SlicedCompilationUnit.SlicedClass includedClassContents = null;
						for (JavaParsingUtils.SlicedCompilationUnit.SlicedClass cl : includedClassCompilationUnit.classes) {
							String name = cl.getName();
							if (name == null) 
								continue;
							name = name.replaceAll("\\s+", "");
							
							if (name.equals(includedClass.getSimpleName()) || name.equals(includedClass.getQualifiedName())) {
								includedClassContents = cl;
								break;
							}
						}
						if (includedClassContents == null) {
							printError(templateInfo.classDeclaration, "Error during inclusion of class " + includedClass + ": couldn't parse source code to find the class");
							return;
						}
						
						templateInfo.additionalImports.add("import " + includedClass.getPackage() + ".*;");
						templateInfo.additionalImports.addAll(includedClassCompilationUnit.importAndOtherHeadElements);
						templateInfo.additionalHeaders.addAll(includedClassContents.getInnerClassesAndVisibleInstanceMethodsDeclarations());
						templateInfo.additionalImplemMembers.addAll(includedClassContents.getVisibleInstanceMethodsImplementations());
						
					} catch (IOException e) {
						printError(templateInfo.classDeclaration, "Error during inclusion of class " + includedClass + ":\n" + e.toString());
					}
				}
			}
		}
	}
	private String evalScript(TemplateInfo templateInfo, String scriptName, String scriptSource, SourcePosition scriptPosition) {
		StringWriter out = new StringWriter();
		StringWriter errOut = new StringWriter();
		final PrintWriter epout = new PrintWriter(errOut);
		try {
			Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new LogChute() {
				public void init(RuntimeServices arg0) throws Exception {}
				public boolean isLevelEnabled(int level) {
					return level == ERROR_ID;
				}
				public void log(int arg0, String arg1) {
					if (arg0 == ERROR_ID)
						epout.println(ERROR_PREFIX + arg1);
				}
				public void log(int arg0, String arg1, Throwable arg2) {}
				
			});
			if (!Velocity.evaluate(context, out, scriptName, new StringReader(scriptSource))) {
				printError(scriptPosition, "Evaluation of script failed.\n\t" + errOut.toString().replaceAll("\n", "\n\t"));
			}
			
		} catch (Exception e) {
			logError(scriptPosition, e);
		}
		return out.toString();
	}
	
	private String readResource(String scriptName, File file, String packageName) throws IOException {
		File f = new File(scriptName);
		if (f.exists())
			return ReadText.readText(f);
		
		f = new File(file.getParentFile(), scriptName);
		if (f.exists())
			return ReadText.readText(f);
		
		String fileStr = file.toString().replace(File.separatorChar, '/');
		String packFile = packageName.replace('.', '/');
		int i = fileStr.lastIndexOf(packFile);
		if (i >= 0) {
			f = new File(fileStr.substring(0, i) + scriptName);
			if (f.exists()) {
				return ReadText.readText(f);
			}
		}
		
		URL url = getClass().getClassLoader().getResource(scriptName);
		if (url == null)
			throw new FileNotFoundException(scriptName);
		
		return ReadText.readText(url);
	}
	void printVeloHelp(Declaration dec) {
		printError(dec, "Inline velocity annotation has to be set on an uninitialized final field with a non-existing type name (such as 'final MyVelocityMethods methods;'");
	}
	
	static class CustomGeneratedInnerClass {
		public String className;
		public String fieldName;
		public String sourceCode;

		public CustomGeneratedInnerClass(String className, String fieldName, String sourceCode) {
			this.className = className;
			this.fieldName = fieldName;
			this.sourceCode = sourceCode;
		}
		
	}
	/*private CustomGeneratedInnerClass processInlineVelocity(Declaration dec) {
		if (!(dec instanceof FieldDeclaration)) {
			printVeloHelp(dec);
			return null;
		}
		FieldDeclaration decl = (FieldDeclaration)dec;
		if (decl.getModifiers().contains(Modifier.PRIVATE) ||
				!decl.getModifiers().contains(Modifier.FINAL) || 
				decl.toString().contains(" = ")) 
		{
			printVeloHelp(dec);
			return null;
		}
	}*/
	private void processTemplateClass(ClassDeclaration classDeclaration) throws IOException 
	{
		AnnotationMirror templateAnnotationMirror = null;
		for (AnnotationMirror annoMirror : classDeclaration.getAnnotationMirrors()) {
			if (annoMirror.getAnnotationType().toString().equals(Template.class.getName())) {
				templateAnnotationMirror = annoMirror;
				break;
			}
		}
		if (templateAnnotationMirror == null) 
			return;
		
		if (classDeclaration.getDeclaringType() != null) {
			printError(templateAnnotationMirror.getPosition(), "Cannot define nested classes as templates.");
			return;
		}
		
		if (!classDeclaration.getModifiers().contains(Modifier.ABSTRACT)) {
			printError(classDeclaration, "Template "+classDeclaration.getQualifiedName()+" must be declared as abstract.");
		}
		
		TemplateInfo templateInfo = new TemplateInfo(classDeclaration);
		
		/// Process Include statements
		processIncludes(templateInfo, templateInfo.classDeclaration.getPosition());
		
		LinesFormatter f = new LinesFormatter(environment.getFiler().createSourceFile(templateInfo.templateInterfaceQualifiedName), "");
		
		f.println(array(
			"//",
			"// This file was autogenerated by " + getClass().getName() + " from " + classDeclaration.getQualifiedName(),
			"//",
			templateInfo.packageName.length() == 0 ? null : "package " + templateInfo.packageName + ";",
			""
		));
		
		for (String s : templateInfo.additionalImports)
			f.println(s);
		
		f.println("interface " + templateInfo.templateInterfaceName + templateInfo.genericParamsDefinition + " extends " + TemplateInstance.class.getName() + " {");
		f.println();
		
		for (FieldDeclaration field : templateInfo.properties) {
			String propertyName = field.getSimpleName();
			
			f.println(field.getDocComment());
			f.format("{0} get{1}();", field.getType(), capitalize(propertyName));
			f.println();
			
			if (!field.getModifiers().contains(Modifier.FINAL)) {
				f.println(field.getDocComment());
				f.format("void set{0}({1} {2});", capitalize(propertyName), field.getType(), propertyName);
				f.println();
			}
		}
		
		for (FieldDeclaration veloField : templateInfo.veloScripts) {
			//String propertyName = veloField.getSimpleName();
			//f.println(veloField.getDocComment());
			
			String script = getDocCommentContent(veloField);
			if (script == null) {
				printError(veloField, "Failed to parse the inline velocity script. Make sure it is right before the @" + InlineVelocity.class.getName() + " annotation.");
				continue;
			}
			StringWriter out = new StringWriter();
			StringWriter errOut = new StringWriter();
			final PrintWriter epout = new PrintWriter(errOut);
			try {
				Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new LogChute() {
					public void init(RuntimeServices arg0) throws Exception {}
					public boolean isLevelEnabled(int level) {
						return level == ERROR_ID;
					}
					public void log(int arg0, String arg1) {
						if (arg0 == ERROR_ID)
							epout.println(ERROR_PREFIX + arg1);
					}
					public void log(int arg0, String arg1, Throwable arg2) {}
					
				});
				if (!Velocity.evaluate(context, out, veloField.toString(), new StringReader(script))) {
					printError(veloField, "Evaluation of Velocity script failed. See generated source for details.");
				}
				String errorString = errOut.toString();
				f.println("/**");
				f.println("* Expansion of inline velocity script");
				f.println(errorString.length() == 0 ? null : "* " +errorString.replaceAll("\n", "\n* "));
				f.println("*/");
				
				f.println("final class " + veloField.getType() + " {");
				f.println(out.toString());
				f.println("}");
				f.println();
			} finally {
				//Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, null);
			}
		}
		
		// Headers for T() and T(int)
		for (String genericParamName : templateInfo.genericParamNames) {
			String lengthName = chooseUniqueName("arraySize", Collections.singleton(genericParamName), false);
			
			f.format(array(
					"/** Get the class of the generic parameter {1} */",
					"{0} {1}();",
					"",
					"/** Create a new array of elements of class {1}.<br/>",
					"    Equivalent to a call to java.lang.reflect.Array.newInstance({2}.{1}(), " + lengthName + ") */",
					"{3}<{1}> " + genericParamName + "(int " + lengthName + ");",
					""
				),
				typedClass(genericParamName),
				genericParamName, 
				templateInfo.templateInterfaceName,
				Array.class.getName()
			);
		}
		
		List<ConstructorInfo> constructorInfos = new ArrayList<ConstructorInfo>();
		for (ConstructorDeclaration originalConstructor : classDeclaration.getConstructors())
			if (originalConstructor.getModifiers().contains(Modifier.PUBLIC))
				constructorInfos.add(new ConstructorInfo(templateInfo, originalConstructor));
		
		for (String h : templateInfo.additionalHeaders) {
			f.println(h);
			f.println();
		}
		
		createFactoryClassCode(f, templateInfo, constructorInfos);
		
		
		f.println("}");
		f.close();
		
		checkTemplateClassGenericParametersMatchItsInterface(templateInfo);
		
		String sourceFile = templateInfo.classDeclaration.getQualifiedName().replace('.', '/') + ".java";
		String source = ReadText.readText(getClass().getClassLoader().getResourceAsStream(sourceFile));
		if (source != null) {
			String simple = templateInfo.classDeclaration.getSimpleName();
			String decoratedName = simple + "__int";
		}
	}

	private String getDocCommentContent(Declaration veloField) {
		String comment = veloField.getDocComment();
		if (comment == null || comment.length() == 0) {
			String source = ReadText.readText(veloField.getPosition().file());
			if (source == null)
				return null;
			
			int offset = computeOffset(source, veloField.getPosition());
			if (offset < 0) {
				printError(veloField, "Failed to parse source comment");
				return null;
			}
			int i = source.lastIndexOf("*/", offset);
			if (i < 0)
				return null;
			
			int k = source.indexOf(";", i);
			if (k >= 0 && k < offset)
				return null;
			
			int j = source.lastIndexOf("/**", i);
			comment = j < 0 ? null : source.substring(j + 3, i - 1);
		}
		if (comment == null)
			return null;
		
		comment = comment.trim();
		if (comment.startsWith("/**"))
			comment = comment.substring(3);
		
		if (comment.endsWith("*/"))
			comment = comment.substring(0, comment.length() - 2);
		
		comment = comment.replaceAll("\n\\s*\\*\\s?+", "\n");
		return comment;
	}
	int computeOffset(String s, SourcePosition pos) {
		int i = 0, l = 1;
		while (l < pos.line()) {
			i = s.indexOf("\n", i);
			if (i >= 0)
				l++;
			else
				return -1;
		}
		return i + pos.column();
	}
	
	private void createImplementationClassCode(LinesFormatter f, TemplateInfo templateInfo, List<ConstructorInfo> ctorInfos) {
		f.println();
		f.println("/// Concrete implementation of " + templateInfo.classDeclaration.getSimpleName() + templateInfo.genericParamsUsage);
		//f.println("@SuppressWarnings(\"unchecked\")");
		f.println("private static " + (templateInfo.genericParamNames.isEmpty() ? "" : "abstract ") +"class " + templateInfo.implemClassName + templateInfo.genericParamsDefinition + " extends " + templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage + " {");
	
		Set<String> existingFields = new TreeSet<String>();
		existingFields.addAll(templateInfo.genericParamNames);
		
		for (ConstructorInfo ctorInfo : ctorInfos) {
			//String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			f.printfn(
				"public " + templateInfo.implemClassName + "(%s) %s {",
				implode(ctorInfo.generatedConstructorArgumentsDeclarations), 
				wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")
			); 
			
			f.println("super(" + implode(ctorInfo.originalCtorArgNames, ", ") + ");");
			
			// Generate properties setting statements
			for (FieldDeclaration propertyField : templateInfo.propertiesToAddToConstructors) {
				String propertyName = propertyField.getSimpleName();
				f.println("this." + propertyName + " = " + ctorInfo.propertiesInitArgNames.get(propertyName) + ";");
			}
			
			for (FieldDeclaration veloField : templateInfo.veloScripts) {
				String veloName = veloField.getSimpleName();
				f.println("this." + veloName + " = new " + veloField.getType() + "();");
			}
			
			f.println("}");
			f.println();
		}
		
		writeGettersAndSetters(f, templateInfo);
		writeGenericParameterConstructingMethodsCode(f, templateInfo);
		
		/// Enable introspection
		
		f.println("public " + TemplateClass.class.getName() + " getTemplate() {");
		Collection<String> callsToTemplateClassMethods = new ArrayList<String>();
		for (String genericParamName : templateInfo.genericParamNames)
			callsToTemplateClassMethods.add(genericParamName + "()");

		f.println("return new " + GENERATED_FACTORY_NAME + "(" + implode(callsToTemplateClassMethods) + ");");
		f.println("}");
		
		for (String i : templateInfo.additionalImplemMembers) {
			f.println();
			f.println(i);
		}
		
		f.println("}");		
	}
	
	private void writeGenericParamRelatedMethods(LinesFormatter f, TemplateInfo templateInfo, ConstructorInfo ctorInfo) {
		// Generate methods T() (generic parameter class getter) and T(int) (array builder)
		for (String genericParamName : templateInfo.genericParamNames) {
			String argName = ctorInfo.genericParamClassArgNames.get(genericParamName);
			f.println(array(
				"public final " + typedClass(genericParamName) + " " + genericParamName + "() { return " + argName + "; }",
				"public final " + Array.class.getName() + "<" + genericParamName + "> " + genericParamName + "(int arraySize) {",
					"return " + ReificationUtils.class.getName() + ".newArray(" + argName + ", arraySize);",
				"}"
			));
		}
		
	}
	private void writeGettersAndSetters(LinesFormatter f, TemplateInfo templateInfo) {
		// Implement properties getters and setters
		for (FieldDeclaration field : templateInfo.properties) {
			String capitalizedName = capitalize(field.getSimpleName());
			
			f.println(field.getDocComment());
			f.format("public {0} get{2}() '{' return {1}; '}'",
					field.getType(),
					field.getSimpleName(),
					capitalizedName);
			f.println();
			
			if (!field.getModifiers().contains(Modifier.FINAL)) {
				f.println(field.getDocComment());
				f.format("public void set{2}({0} {1}) '{' this.{1} = {1}; '}'",
						field.getType(),
						field.getSimpleName(),
						capitalizedName);
				f.println();
			}
		}
	}
	private void createFactoryClassCode(LinesFormatter f, TemplateInfo templateInfo, List<ConstructorInfo> ctorInfos) {
		f.println("/// Factory class for " + templateInfo.classDeclaration.getSimpleName() + templateInfo.genericParamsUsage);
		f.println("@SuppressWarnings(\"unchecked\")");
		f.println("public final static class " + GENERATED_FACTORY_NAME + " extends " + AbstractTemplateClass.class.getName() + " {");
		
		f.println(array(
				"",
				"protected " + GENERATED_FACTORY_NAME + "(Class<?>... genericTypes) { super(genericTypes); }",
				""
		));
		for (ConstructorInfo ctorInfo : ctorInfos) {
			
			String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			
			f.printfn(
					"public static final %s %s newInstance(%s) %s {",
					templateInfo.genericParamsDefinition,
					qualifiedTemplateInterfaceNameWithGenericsUsage,
					implode(ctorInfo.generatedFactoryArgumentsDeclarations), 
					wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")); 
			
			String instanceName = chooseUniqueName("instance", ctorInfo.existingArgumentNames, false);
			if (!templateInfo.genericParamNames.isEmpty()) {
				f.printfn("%s %s = new %s(%s) {",
					templateInfo.qualifiedTemplateNameWithGenericsUsage,
					instanceName,
					templateInfo.implemClassName + templateInfo.genericParamsUsage,
					implode(ctorInfo.generatedConstructorArguments)
				);
				
				writeGenericParamRelatedMethods(f, templateInfo, ctorInfo);
				//createImplementationClassCode(f, templateInfo, ctorInfos, true, ctorInfo.genericParamClassArgNames);
				
				f.println("};");
				f.println("return " + instanceName + ";");
				f.println("}");
			} else {
				f.printfn("return new %s(%s);",
					templateInfo.implemClassName + templateInfo.genericParamsUsage,
					implode(ctorInfo.generatedConstructorArguments)
				);
				f.println("};");
			}
		}
		
		createImplementationClassCode(f, templateInfo, ctorInfos);
		
		/*for (ConstructorInfo ctorInfo : ctorInfos) {
			
			String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			f.printfn(
				"public static final %s %s newInstance(%s) %s {",
				templateInfo.genericParamsDefinition,
				qualifiedTemplateInterfaceNameWithGenericsUsage,
				implode(ctorInfo.generatedConstructorArgumentsDeclarations), 
				wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")); 
					 
			String instanceName = chooseUniqueName("instance", ctorInfo.existingArgumentNames, false);
			f.println("return new " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage + "(" + implode(ctorInfo.generatedConstructorArguments) + ");");
			f.println("}");
		}
		//createImplementationClassCode(f, templateInfo, ctorInfos, false, null);
		*/
		/*
		for (ConstructorInfo ctorInfo : ctorInfos) {
			
			String qualifiedTemplateInterfaceNameWithGenericsUsage = templateInfo.classDeclaration.getQualifiedName() + templateInfo.genericParamsUsage;
			f.println(ctorInfo.constructorDeclaration.getDocComment());
			f.printfn(
				"public static final %s %s newInstance(%s) %s {",
				templateInfo.genericParamsDefinition,
				qualifiedTemplateInterfaceNameWithGenericsUsage,
				implode(ctorInfo.generatedConstructorArgumentsDeclarations), 
				wrappedImplosion(ctorInfo.constructorDeclaration.getThrownTypes(), "throws ", "")); 
					 
			String instanceName = chooseUniqueName("instance", ctorInfo.existingArgumentNames, false);
			f.printfn("%s %s = new %s(%s) {",
				templateInfo.qualifiedTemplateNameWithGenericsUsage,
				instanceName,
				templateInfo.qualifiedTemplateNameWithGenericsUsage,
				implode(ctorInfo.generatedConstructorArguments)
			);
			
			createImplementationClassCode(f, templateInfo, ctorInfos, true, ctorInfo.genericParamClassArgNames);
			
			f.println("};");
			
			f.println("return new " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage + "(" + implode(ctorInfo.generatedConstructorArguments) + ");");
			//f.println("return new " + GENERATED_IMPLEMENTATION_NAME + templateInfo.genericParamsUsage + "(" + implode(ctorInfo.generatedConstructorArguments) + ");");
			f.println("}");
		}*/
		
		
		
		f.println("}");		
	}

	private void checkTemplateClassGenericParametersMatchItsInterface(TemplateInfo templateClassInfo) {

		String fullTemplateInterfaceImplementsReference = (templateClassInfo.templateInterfaceQualifiedName + templateClassInfo.genericParamsUsage).replaceAll("\\s+", "");
		boolean found = false;
		for (InterfaceType inter : templateClassInfo.classDeclaration.getSuperinterfaces()) {
			TypeMirror erased = environment.getTypeUtils().getErasure(inter);
			if (erased.toString().equals(templateClassInfo.templateInterfaceQualifiedName)) {
				if (!inter.toString().replaceAll("\\s+", "").equals(fullTemplateInterfaceImplementsReference)) {
					printError(templateClassInfo.classDeclaration, "Template class " + templateClassInfo.classDeclaration.getSimpleName() + templateClassInfo.genericParamsDefinition + " must implement its template interface with the same parameters. Expecting 'implements " + templateClassInfo.templateInterfaceName + templateClassInfo.genericParamsUsage+"'");
				}
				found = true;
			}
		}
		if (!found)
			printWarning(templateClassInfo.classDeclaration, "Template class " + templateClassInfo.classDeclaration.getSimpleName() + templateClassInfo.genericParamsDefinition + " must implement its template interface with the same parameters. Expecting 'implements " + templateClassInfo.templateInterfaceName + templateClassInfo.genericParamsUsage+"'");
	}
	
	/**
	Generate generic parameter constructing methods, based on param constructor contracts such as :
 	@@Initializer T new_T(...) throws ...;
	*/
	private void writeGenericParameterConstructingMethodsCode(LinesFormatter f, TemplateInfo templateClassInfo) {
		for (Map.Entry<String, Pair<MethodDeclaration, List<MethodDeclaration>>> e : templateClassInfo.paramConstructorContracts.entrySet()) {
			String paramName = e.getKey();
			
			MethodDeclaration neutralValueDeclaration = e.getValue().getFirst();
			if (neutralValueDeclaration != null) {
				f.println(neutralValueDeclaration.getDocComment());
				f.println("@SuppressWarnings(\"unchecked\")");
				f.println("public " + paramName + " " + neutralValueDeclaration.getSimpleName() + "() {");
				f.println("return " + ReificationUtils.class.getName() + ".getNeutralValue(" + paramName + "());");
				//f.println("return " + paramName + "().isPrimitive() ? (" + paramName + ")0 : null;");
				f.println("}");
			}
			for (MethodDeclaration constructorContract : e.getValue().getSecond()) {
				String contractThrowsClause = wrappedImplosion(constructorContract.getThrownTypes(), "throws ", "");
					
				String paramConstructorArgsDef = implode(constructorContract.getParameters());
				f.println(constructorContract.getDocComment());
				f.println("@SuppressWarnings(\"unchecked\")");
				f.println("public " + paramName + " " + constructorContract.getSimpleName() + "(" + paramConstructorArgsDef + ") " + contractThrowsClause + " {");
				
				List<String> paramConstructorArgsTypes = new ArrayList<String>();
				List<String> paramConstructorArgsNames = new ArrayList<String>();
				for (ParameterDeclaration d : constructorContract.getParameters()) {
					String type = d.getType().toString();
					if (templateClassInfo.genericParamNames.contains(type)) {
						paramConstructorArgsTypes.add(d.getType() + "()");
					} else {
						paramConstructorArgsTypes.add(d.getType() + ".class");
					}
					paramConstructorArgsNames.add(d.getSimpleName());
				}

				Set<String> existingArgNames = new TreeSet<String>(paramConstructorArgsNames);
				
				String exName = chooseUniqueName("ex", existingArgNames, true),
					innerEx = chooseUniqueName("innerEx", existingArgNames, true),
					innerExClass = chooseUniqueName("innerExClass", existingArgNames, true);
				
				f.println(array(
					"try {",
					"return " + ReificationUtils.class.getName() + ".newInstance(" + paramName +"(), new Class[] {" + implode(paramConstructorArgsTypes) + "}, new Object[] {" + implode(paramConstructorArgsNames) + "});",
					//"return (" + paramName +")" + paramName + "().getConstructor(" + implode(paramConstructorArgsTypes) + ").newInstance(" + implode(paramConstructorArgsNames)+");",
					"} catch (" + SecurityException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Cannot access to the constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ")\", " + exName + ");",
					"} catch (" + IllegalAccessException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Cannot invoke the constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ") \", " + exName + ");",
					"} catch (" + NoSuchMethodException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"The expected constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ") does not exist\", " + exName + ");",
					"} catch (" + IllegalArgumentException.class.getName() + " " + exName + ") {",
					"	throw new " + RuntimeException.class.getName() + "(\"Internal Jeneral exception\", " + exName + ");",
					"} catch (" + InstantiationException.class.getName() + " " + exName + ") {",
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Template parameter class \" + " + paramName + "().getName() + \" is abstract and cannot be instantiated\", " + exName + ");",
					"} catch (" + InvocationTargetException.class.getName() + " " + exName + ") {",
					"	" + Throwable.class.getName() + " " + innerEx + " = " + exName + ".getCause();",
					"	" + typedClass("? extends " + Throwable.class.getName()) + " " + innerExClass + " = " + innerEx + ".getClass();"
				));
				f.println("if (" + RuntimeException.class.getName() + ".class.isAssignableFrom(" + innerExClass + ")) throw (" + RuntimeException.class.getName() + ")" + innerEx + ";");
				for (ReferenceType expectedException : constructorContract.getThrownTypes())
					f.println("if (" + expectedException + ".class.isAssignableFrom(" + innerExClass + ")) throw (" + expectedException + ")" + innerEx + ";");
				
				f.println(array(
					"	throw new " + TemplateContractViolationException.class.getName() + "(\"Template parameter constructor \" + " + paramName + "().getName() + \"(" + implode(paramConstructorArgsTypes) + ") threw a undeclared checked exception of type \" + " + innerEx + ".getClass().getName(), " + innerEx + ");",
					"}"
				));
				
				f.println("}");
			}
		}
	}
	
	/**
	Get the list of generic parameter constructing methods contracts such as :
 	@@Initializer T new_T(...) throws ...;
	*/
	private Map<String, Pair<MethodDeclaration, List<MethodDeclaration>>> getGenericParamConstructorsContracts(ClassDeclaration decl, List<String> genericParamNames) {
		Map<String, Pair<MethodDeclaration, List<MethodDeclaration>>> paramConstructorContractsByParam = new HashMap<String, Pair<MethodDeclaration,List<MethodDeclaration>>>();
		for (MethodDeclaration constructorContract : decl.getMethods()) {
			ParamConstructor declAnn = constructorContract.getAnnotation(ParamConstructor.class);
			if (declAnn == null)
				continue;
			
			boolean canReturnNeutralValue = constructorContract.getParameters().size() == 0;
			
			if (declAnn.returnNeutralValue() && !canReturnNeutralValue) {
				printError(constructorContract, "Neutral values may only be returned from initializers with no argument.");
			}
			
			if (!constructorContract.getModifiers().contains(Modifier.ABSTRACT)) {
				printError(constructorContract, "Template parameter constructor declarations must be abstract methods !");
				continue;
			}
			TypeMirror m = constructorContract.getReturnType();
			String paramType = m.toString();
			if (!genericParamNames.contains(paramType)) {
				printError(constructorContract, "Template parameter constructor declarations must have a return type that matches one of the template parameters !");
				continue;
			}
			if (!constructorContract.getFormalTypeParameters().isEmpty()) {
				printError(constructorContract, "Template parameter constructor declarations cannot be generics");
				continue;
			}
			Pair<MethodDeclaration, List<MethodDeclaration>> pair = paramConstructorContractsByParam.get(paramType);
			if (pair == null) {
				pair = new Pair<MethodDeclaration, List<MethodDeclaration>>(null, new ArrayList<MethodDeclaration>());
				paramConstructorContractsByParam.put(paramType, pair);
			}
			if (declAnn.returnNeutralValue()) {
				if (pair.getFirst() != null) {
					printError(constructorContract, "Duplicate neutral value getter for parameter " + paramType);
					printError(pair.getFirst(), "Duplicate neutral value getter for parameter " + paramType);
				} else {
					pair.setFirst(constructorContract);
				}
				continue;
			}
			List<MethodDeclaration> constructorContracts = pair.getSecond(); 
			
			boolean hasDuplicates = false;
			
			String paramConstructorArgs = constructParameterTypesString(constructorContract);
			for (MethodDeclaration existingContract : constructorContracts) {
				if (constructParameterTypesString(existingContract).equals(paramConstructorArgs)) {
					printError(constructorContract, "Duplicate constructor contract for parameter " + paramType);
					printError(existingContract, "Duplicate constructor contract for parameter " + paramType);
					hasDuplicates = true;
				}
			}
			if (!hasDuplicates)
				constructorContracts.add(constructorContract);
		}
		return paramConstructorContractsByParam;
	}

	private String constructParameterTypesString(MethodDeclaration constructorContract) {
		List<String> list = new ArrayList<String>();
		for (ParameterDeclaration d : constructorContract.getParameters()) {
			list.add(d.getType().toString());
		}
		return implode(list);
	}

	private void processInstantiation(Declaration decl) {
		Instantiate instantiation = decl.getAnnotation(Instantiate.class);
		Template template = instantiation.template().getAnnotation(Template.class);
		if (template == null) {
			printError(decl, "Type " + instantiation.template().getName() + " is not a template. You must annotate it with " + Template.class.getName());
			return;
		}
	}
}

