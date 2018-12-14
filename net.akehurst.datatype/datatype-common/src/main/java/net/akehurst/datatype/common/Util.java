/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.datatype.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;

public class Util {

	public static void wrap(final Runnable action) {
		try {
			action.run();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T wrap(final Callable<T> action) {
		try {
			return action.call();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getMemberName(final Method accessor) {
		return accessor.getName().substring(3, 4).toLowerCase() + accessor.getName().substring(4);
	}

	public static Method getMutator(final Method accessor) {
		final String muName = "set" + accessor.getName().substring(3);
		final Class<?> type = accessor.getReturnType();
		try {
			return accessor.getDeclaringClass().getMethod(muName, type);
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	public static boolean isDatatype(final Class<?> cls) {
		if (null == cls) {
			return false;
		}
		final Datatype ann = cls.getAnnotation(Datatype.class);
		if (null == ann) {
			// check interfaces, superclasses are included because @Datatype is marked as @Inherited
			for (final Class<?> intf : cls.getInterfaces()) {
				if (Util.isDatatype(intf)) {
					return true;
				}
			}
			// check interfaces of superclass
			return Util.isDatatype(cls.getSuperclass());
		} else {
			return true;
		}
	}

	public static boolean isNavigableAccessor(final Method m) {
		boolean res = true;
		res &= null != m.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
		res &= null == m.getDeclaredAnnotation(Query.class);
		res &= null == m.getDeclaredAnnotation(Reference.class);
		res &= 0 == m.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
		res &= m.getName().startsWith("get");
		return res;
	}

	public static boolean isIdentityAccessor(final Method m) {
		boolean res = true;
		res &= null != m.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
		res &= null != m.getDeclaredAnnotation(Identity.class);
		return res;
	}

	public static boolean isPropertyAccessor(final Method m) {
		boolean res = true;
		res &= null != m.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
		res &= null == m.getDeclaredAnnotation(Identity.class);
		res &= null == m.getDeclaredAnnotation(Query.class);
		res &= 0 == m.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
		res &= m.getName().startsWith("get");
		return res;
	}

	public static Set<Method> getNavigableMethods(final Class<?> cls) {
		if (null == cls) {
			return new HashSet<>();
		} else {
			final Set<Method> result = new HashSet<>();
			final Set<Method> superclassMethods = Util.getNavigableMethods(cls.getSuperclass());
			result.addAll(superclassMethods);
			for (final Class<?> intf : cls.getInterfaces()) {
				final Set<Method> interfaceMethods = Util.getNavigableMethods(intf);
				result.addAll(interfaceMethods);
			}
			for (final Method m : cls.getDeclaredMethods()) {
				if (Util.isNavigableAccessor(m)) {
					result.add(m);
				}
			}
			return result;
		}
	}

	public static List<String> createPath(final Object from, final Object to) {
		if (null == from) {
			return null;
		} else if (from == to) {
			return new ArrayList<>();
		} else {
			if (from instanceof Collection<?>) {
				final Collection<?> arr = (Collection<?>) from;
				int i = 0;
				for (final Object o : arr) {
					final List<String> path = Util.createPath(o, to);
					if (null != path) {
						path.add(0, Integer.toString(i));
						return path;
					}
					++i;
				}
				return null;
			} else if (Util.isDatatype(from.getClass())) { // treat from as an Object
				for (final Method m : Util.getNavigableMethods(from.getClass())) {
					final Object value = Util.wrap(() -> m.invoke(from));
					final List<String> path = Util.createPath(value, to);
					if (null != path) {
						path.add(0, Util.getMemberName(m));
						return path;
					}
				}
				return null;
			} else {
				return null;
			}
		}
	}
}
