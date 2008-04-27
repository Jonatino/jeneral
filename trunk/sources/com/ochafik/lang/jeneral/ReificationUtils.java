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

public class ReificationUtils {
	
	@SuppressWarnings("unchecked")
	public static <T> Array<T> newArray(Class<T> c, final int length) {
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
		else if (c == Long.TYPE) {
			return new Array<T>() {
				final long[] array = new long[length];
				public void set(int i, T value) { 
					array[i] = ((Long)value).longValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Long)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final short[] array = new short[length];
				public void set(int i, T value) { 
					array[i] = ((Short)value).shortValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Short)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final byte[] array = new byte[length];
				public void set(int i, T value) { 
					array[i] = ((Byte)value).byteValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Byte)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final char[] array = new char[length];
				public void set(int i, T value) { 
					array[i] = ((Character)value).charValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Character)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final float[] array = new float[length];
				public void set(int i, T value) { 
					array[i] = ((Float)value).floatValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Float)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final double[] array = new double[length];
				public void set(int i, T value) { 
					array[i] = ((Double)value).doubleValue(); 
				}
				@SuppressWarnings("unchecked")
				public T get(int i) { 
					return (T)(Double)array[i]; 
				}
				public Object getArray() { return array; }
				public int length() { return array.length; }
			};
		}
		throw new UnsupportedOperationException();
	}
}
