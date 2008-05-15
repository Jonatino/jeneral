package com.ochafik.lang.jeneral.examples;

import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.runtime.Array;

@Template
public abstract class TDynamicArray<T> implements _TDynamicArray<T> {
	private Array<T> array;
	
	public TDynamicArray(int size) {
		array = T(size);
	}
	
	
	public void enlarge(int minSize) {
		if (minSize <= array.length()) return;
		setSize(minSize);
	}
	public void setSize(int size) {
		if (size == array.length()) return;
		
		Array<T> newArray = T(size);
		int oldSize = array.length();
		
		System.arraycopy(array.getArray(), 0, newArray.getArray(), 0, oldSize < size ? oldSize : size);
		array = newArray;
	}
	
	public int getSize() {
		return array.length();
	}
	
	public void set(int pos, T value) {
		array.set(pos, value);
	}
	
	public T get(int pos) {
		return array.get(pos);
	}
}
