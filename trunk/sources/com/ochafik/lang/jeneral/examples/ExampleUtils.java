package com.ochafik.lang.jeneral.examples;
import com.ochafik.lang.jeneral.annotations.*;
public class ExampleUtils {
	@Inline
	public static String capitalize(String s) {
		char[] chars = s.toCharArray();
		if (chars.length == 0)
			return "";
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars); 
	}

}
