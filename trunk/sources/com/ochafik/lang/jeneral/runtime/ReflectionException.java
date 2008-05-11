/**
 * 
 */
package com.ochafik.lang.jeneral.runtime;

public class ReflectionException extends Exception {
	private static final long serialVersionUID = 2431641373888121584L;

	public ReflectionException(String text) {
		super(text);
	}
	public ReflectionException(Throwable cause) {
		super(cause);
	}
}