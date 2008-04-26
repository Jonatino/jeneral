package com.ochafik.lang.jeneral;

class DefaultArray<T> implements Array<T> {
	T[] array;
	public DefaultArray(T[] array) {
		if (array == null)
			throw new NullPointerException("null array !");
		if (!array.getClass().isArray())
			throw new IllegalArgumentException("Not an array : "+array);
		
		this.array = array;
	}
	
	@SuppressWarnings("unchecked")
	public T get(int i) {
		return (T)java.lang.reflect.Array.get(array, i);
	}

	public T[] getArray() {
		return array;
	}

	public int length() {
		return java.lang.reflect.Array.getLength(array);
	}

	public void set(int i, T value) {
		java.lang.reflect.Array.set(array, i, value);
	}

}
