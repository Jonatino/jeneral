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

import static com.ochafik.util.string.StringUtils.implode;

import java.lang.reflect.InvocationTargetException;

public class ReificationUtils {
	
	@SuppressWarnings("unchecked")
	public static <T> T getNeutralValue(Class<T> c) {
		if (!c.isPrimitive())
			return null;
		else if (c == Integer.TYPE)
			return (T)new Integer(0);
		else if (c == Long.TYPE)
			return (T)new Long(0);
		else if (c == Double.TYPE)
			return (T)new Double(0);
		else if (c == Short.TYPE)
			return (T)new Short((short)0);
		else if (c == Byte.TYPE)
			return (T)new Byte((byte)0);
		else if (c == Character.TYPE)
			return (T)new Character((char)0);
		else if (c == Float.TYPE)
			return (T)new Float(0);
			
		throw new UnsupportedOperationException();
	}
	
	public static <T> T newInstance(Class<T> c, Class<?>[] argTypes, Object[] args) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (!c.isPrimitive())
			return (T)c.getConstructor(argTypes).newInstance(args);
		
		if (argTypes.length != args.length)
			throw new IllegalArgumentException();
		
		if (argTypes.length > 1)
			throw new NoSuchMethodException("Primitive cannot be constructed with more than one value");
		
		if (argTypes.length == 0) {
			return getNeutralValue(c);
		}
		Class<?> cc = argTypes[0];
		if (!c.isAssignableFrom(cc))
			throw new NoSuchMethodException(c + " cannot be constructed from a " + cc);
			
		Object a = args[0];
 		if (c == Integer.TYPE)
			return (T)(Integer)a;
		else if (c == Long.TYPE)
			return (T)(Long)a;
		else if (c == Double.TYPE)
			return (T)(Double)a;
		else if (c == Short.TYPE)
			return (T)(Short)a;
		else if (c == Byte.TYPE)
			return (T)(Byte)a;
		else if (c == Character.TYPE)
			return (T)(Character)a;
		else if (c == Float.TYPE)
			return (T)(Float)a;
			
		throw new UnsupportedOperationException();
		
	}
	//	"return (" + paramName +")" + paramName + "().getConstructor(" + implode(paramConstructorArgsTypes) + ").newInstance(" + implode(paramConstructorArgsNames)+");",
	
	@SuppressWarnings("unchecked")
	public static <T> Array<T> newArray(Class<T> c, final int length) {
		if (!c.isPrimitive())
			return new NonPrimitiveArray<T>((T[])java.lang.reflect.Array.newInstance(c, length));
		else if (c == Integer.TYPE) {
			return new Array<T>() {
				final int[] array = new int[length];
				public T set(int i, T value) {
					int pv = ((Integer)value).intValue();
					Integer ov = pv;
					array[i] = pv; 
					return (T)ov;
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
				public T set(int i, T value) { 
					long pv = ((Long)value).longValue();
					Long ov = pv;
					array[i] = pv; 
					return (T)ov;
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
				public T set(int i, T value) { 
					short pv = ((Short)value).shortValue();
					Short ov = pv;
					array[i] = pv; 
					return (T)ov;
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
				public T set(int i, T value) { 
					byte pv = ((Byte)value).byteValue();
					Byte ov = pv;
					array[i] = pv; 
					return (T)ov;
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
				public T set(int i, T value) { 
					char pv = ((Character)value).charValue();
					Character ov = pv;
					array[i] = pv; 
					return (T)ov;
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
				public T set(int i, T value) { 
					float pv = ((Float)value).floatValue();
					Float ov = pv;
					array[i] = pv; 
					return (T)ov;
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
				public T set(int i, T value) { 
					double pv = ((Double)value).doubleValue();
					Double ov = pv;
					array[i] = pv; 
					return (T)ov;
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
