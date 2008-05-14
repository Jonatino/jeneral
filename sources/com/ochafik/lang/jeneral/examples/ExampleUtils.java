package com.ochafik.lang.jeneral.examples;

public class ExampleUtils {

	public static String capitalize(String s) {
		char[] chars = s.toCharArray();
		if (chars.length == 0)
			return "";
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars); 
	}

}
