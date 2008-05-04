/**
 * 
 */
package com.ochafik.lang.jeneral.processors;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Stack;

public class LinesFormatter {
	PrintWriter out;
	Stack<String> indentStack = new Stack<String>();
	String currentIndent;
	
	public LinesFormatter(PrintWriter out, String initialIndent) {
		this.out = out;
		indentStack.push(initialIndent);
		currentIndent = initialIndent;
	}
	
	public void printfn(String fmt, Object... args) {
		println(String.format(fmt, args));
	}
	public void println() {
		out.println();
	}
	
	public <T> void println(T[] arr) {
		for (T t : arr)
			println(t);
	}
	public void println(Object o) {
		if (o == null)
			return;
		
		String ss = o.toString();
		for (String s : ss.split("\n")) {
			s = s.trim();
			if (s.startsWith("}")) {
				indentStack.pop();
				currentIndent = indentStack.empty() ? "" : indentStack.lastElement();
			}
			
			out.print(currentIndent);
			out.println(s);
			
			if (s.endsWith("{"))
				indentStack.push(currentIndent = currentIndent + "\t");
		}
	}
	
	public void close() {
		out.close();
	}
	public void format(String[] strings, Object... args) {
		for (String ff : strings)
			format(ff, args);
	}
	public void format(String f, Object... args) {
		if (f == null)
			return; 
		
		try {
			println(MessageFormat.format(f, args));
		} catch (Throwable t) {
			throw new RuntimeException("During formatting of "+f, t);
		}
	}
}