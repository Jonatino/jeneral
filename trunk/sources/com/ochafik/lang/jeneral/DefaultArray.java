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
package com.ochafik.lang.jeneral;

class DefaultArray<T> implements Array<T> {
	Object array;
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

	public Object getArray() {
		return array;
	}

	public int length() {
		return java.lang.reflect.Array.getLength(array);
	}

	public T set(int i, T value) {
		java.lang.reflect.Array.set(array, i, value);
		return value;
	}

	public void copyFrom(Array<T> source, int sourceOffset, int destOffset, int length) {
		System.arraycopy(source.getArray(), sourceOffset, getArray(), destOffset, length);
	}
}
