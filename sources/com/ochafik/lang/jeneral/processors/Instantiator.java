package com.ochafik.lang.jeneral.processors;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.ochafik.io.ReadText;
import com.ochafik.lang.jeneral.examples.ElementsBuilder;
import com.ochafik.util.string.RegexUtils;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;

public class Instantiator {
	public Instantiator(AnnotationProcessorEnvironment env, TemplateProcessor.TemplateInfo templateInfo, TemplateProcessor.ConstructorInfo ctorInfos) {
		//replace(templateInfo.classDeclaration.getQualifiedName(), templateInfo.genericParamNames);
	}
	static String rep(String source, String pattern, String repl) {
		return RegexUtils.regexReplace(Pattern.compile(pattern), source, new MessageFormat(repl));
	}
	private static void replace(String qualifiedName, Map<String, Object> values) {
		try {
			String file = "/Users/ochafik/Prog/Java/sources/" + qualifiedName.replace(".", "/") + ".java";
			String source = ReadText.readText(new File(file));//SmallJavaParser.class.getClassLoader().getResourceAsStream(file));
			MessageFormat replacement = new MessageFormat("");
			//List<Token> tokens = tokens(source);
			//System.out.println(tokens);
			
			for (Map.Entry<String, Object> e : values.entrySet()) {
				String param = e.getKey(), value = e.getValue().toString();
				Collection<String> variables = RegexUtils.find(source, "Array\\s*<\\s*" + param + "\\s*>\\s*(\\w+)\\s*[,;=]", 1);
				
				System.out.println("Replacing "+param);
				System.out.println("Variables : "+variables);
				
				source = rep(source, "@\\s*Template", "");
				source = rep(source, "@\\s*ParamConstructor.*;", "");
				
				source = rep(source, "([^\\w])Array\\s*<\\s*" + param + "\\s*>", "{1}" + value + "[]");
				for (String variable : variables)
					source = rep(source, "([^\\w])" + variable + "\\s*.\\s*get\\s*\\(([^(){}\\[\\]]+)\\)", "{1}" +value + "[{2}]");
				
				source = rep(source, "<\\s*" + param + "\\s*>", "");
				source = rep(source, "([^\\w])" + param + "([^\\w])", "{1}" +value + "{2}");
			}
			
			int insertionPoint = source.indexOf("_Template");
			insertionPoint = source.indexOf("{", insertionPoint) + 1;
			
			StringBuilder ins = new StringBuilder();
			// \@\s*Property(\([^\)]+\))?
			//for (String[] m : RegexUtils.find(source, "@\\s*Property(\\(?:[^)]+\\))?\\s*(\\w+)?\\s+(\\w+)\\s+(\\w+)\\s*[=;]")) {
			for (String[] m : RegexUtils.find(source, "\\@\\s*Property(\\([^\\)]+\\))?\\s*(\\w+)?\\s+(\\w+)\\s+(\\w+)\\s*[=;]")) {
				String mod = m[2], type = m[3], name = m[4];
				String cname = TemplateProcessor.capitalize(name);
				ins.append("\n\tpublic " + type + " get" + cname + "() { return " + name + "; }");
				ins.append("\n\tpublic void set" + cname + "(" + type + " " + name + ") { this." + name + " = " + name + "; }\n");
			}
			
			source = source.substring(0, insertionPoint) + "\n" + ins + source.substring(insertionPoint);
			
			
			//source = RegexUtils.regexReplace(Pattern.compile(param + "\\s*.\\s*get\\s*\\(([^(){}\\[\\]]+)\\)"), source, new MessageFormat(value + "[{1}]"));
			System.out.println(source);
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static String getClassLitt(String type) {
		String lit = primLitterals.get(type);
		if (lit == null)
			lit = type + ".class";
		return lit;
	}
	static final Map<String, String> primLitterals = new HashMap<String, String>();
	static {
		primLitterals.put("int", "Integer.TYPE");
		primLitterals.put("long", "Long.TYPE");
		primLitterals.put("short", "Short.TYPE");
		primLitterals.put("byte", "Byte.TYPE");
		primLitterals.put("char", "Character.TYPE");
		primLitterals.put("double", "Double.TYPE");
		primLitterals.put("float", "Float.TYPE");
	}
	public static void main(String[] args) {
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("T", "int");
		replace(ElementsBuilder.class.getName(), values);
	}
}
