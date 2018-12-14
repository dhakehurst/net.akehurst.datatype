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
package net.akehurst.datatype.common.model;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;
import net.akehurst.datatype.api.DatatypeException;

public class DatatypeProperty {

	private final Method accessor;
	private final String name;
	private final boolean isIdentity;
	private final boolean isComposite;
	private final boolean isReference;
	private final int identityIndex;

	public DatatypeProperty(final Method accessor, final String name, final boolean isIdentity, final int identityIndex, final boolean isComposite,
			final boolean isReference) {
		this.accessor = accessor;
		this.name = name;
		this.isIdentity = isIdentity;
		this.isComposite = isComposite;
		this.isReference = isReference;
		this.identityIndex = identityIndex;
	}

	public DatatypeProperty(final Method accessor) {
		this.accessor = accessor;
		this.name = this.calcName(accessor);
		this.isIdentity = this.calcIsIdentity(accessor);
		this.isComposite = this.calcIsComposite(accessor);
		this.isReference = this.calcIsReference(accessor);
		this.identityIndex = this.calcIdentityIndex(accessor);
	}

	public String getName() {
		return this.name;
	}

	public int getIdentityIndex() {
		return this.identityIndex;
	}

	public boolean isIdentity() {
		return this.isIdentity;
	}

	public boolean isComposite() {
		return this.isComposite;
	}

	public boolean isReference() {
		return this.isReference;
	}

	public Class<?> getType() {
		return this.accessor.getReturnType();
	}

	public <T> T getValueFrom(final Object obj) {
		try {
			return (T) this.accessor.invoke(obj);
		} catch (final Exception e) {
			throw new DatatypeException("Error getting value from object for property " + this.getName(), e);
		}
	}

	public void setValueFor(final Object obj, final Object value) {
		try {
			final Method mutator = this.calcMutator();
			if (List.class.isAssignableFrom(this.getType())) {
				if (null == mutator) {
					final List lv = this.getValueFrom(obj);
					lv.addAll((List) value);
				} else {
					mutator.invoke(obj, value);
				}
			} else if (Set.class.isAssignableFrom(this.getType())) {
				if (null == mutator) {
					final Set lv = this.getValueFrom(obj);
					lv.addAll((Set) value);
				} else {
					mutator.invoke(obj, value);
				}
			} else if (Map.class.isAssignableFrom(this.getType())) {
				if (null == mutator) {
					final Map lv = this.getValueFrom(obj);
					lv.putAll((Map) value);
				} else {
					mutator.invoke(obj, value);
				}
			} else {
				mutator.invoke(obj, value);
			}
		} catch (final Exception e) {
			throw new DatatypeException("Unable to set property value", e);
		}
	}

	private String calcName(final Method accessor) {
		return accessor.getName().substring(3, 4).toLowerCase() + accessor.getName().substring(4);
	}

	private boolean calcIsIdentity(final Method accessor) {
		boolean res = true;
		// res &= null != accessor.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
		res &= null != accessor.getDeclaredAnnotation(Identity.class);
		res &= 0 == accessor.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for
														// e.g. maps!
		res &= accessor.getName().startsWith("get");
		return res;
	}

	private boolean calcIsReference(final Method accessor) {
		boolean res = true;
		// res &= null != accessor.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
		res &= null != accessor.getDeclaredAnnotation(Reference.class);
		res &= 0 == accessor.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for
														// e.g. maps!
		res &= accessor.getName().startsWith("get");
		return res;
	}

	private boolean calcIsComposite(final Method accessor) {
		boolean res = true;
		// res &= null != accessor.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
		res &= null == accessor.getDeclaredAnnotation(Identity.class);
		res &= null == accessor.getDeclaredAnnotation(Query.class);
		res &= null == accessor.getDeclaredAnnotation(Reference.class);
		res &= 0 == accessor.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for
														// e.g. maps!
		res &= accessor.getName().startsWith("get");
		return res;
	}

	private int calcIdentityIndex(final Method accessor) {
		final Identity ann = accessor.getDeclaredAnnotation(Identity.class);
		if (null == ann) {
			return -1;
		} else {
			return ann.value();
		}
	}

	private Method calcMutator() {
		final String muName = "set" + this.accessor.getName().substring(3);
		final Class<?> type = this.accessor.getReturnType();
		try {
			return this.accessor.getDeclaringClass().getMethod(muName, type);
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return this.accessor.getDeclaringClass().getSimpleName() + "." + this.name + " : " + this.getType().getSimpleName();
	}

}
