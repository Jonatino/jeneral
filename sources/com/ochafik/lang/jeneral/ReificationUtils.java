package com.ochafik.lang.jeneral;

public class ReificationUtils {
	
	public static <T> Array<T> new_array(Class<T> c, final int length) {
		if (!c.isPrimitive())
			return new DefaultArray<T>((T[])java.lang.reflect.Array.newInstance(c, length));
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final int[] array = new int[length];
				public void set(int i, T value) { 
					array[i] = ((Integer)value).intValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Integer)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		throw new UnsupportedOperationException();
	}
}
