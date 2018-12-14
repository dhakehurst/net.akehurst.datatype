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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hjson.JsonObject;
import org.hjson.JsonValue;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.api.DatatypeException;

public class DatatypeRegistry {

	public static boolean isDatatype(final Class<?> cls) {
		if (null == cls) {
			return false;
		}
		final Datatype ann = cls.getAnnotation(Datatype.class);
		if (null == ann) {
			// check interfaces, superclasses are included because @Datatype is marked as @Inherited
			for (final Class<?> intf : cls.getInterfaces()) {
				if (DatatypeRegistry.isDatatype(intf)) {
					return true;
				}
			}
			// check interfaces of superclass
			return DatatypeRegistry.isDatatype(cls.getSuperclass());
		} else {
			return true;
		}
	}

	public static boolean isProperty(final Method m) {
		if (null == m) {
			return false;
		}
		if (!DatatypeRegistry.isDatatype(m.getDeclaringClass())) {
			return false;
		}
		if (0 != m.getParameterCount()) {
			return false;
		}
		if (!m.getName().startsWith("get")) {
			return false;
		}
		return true;
	}

	private final Map<Class<?>, DatatypeInfo> datatypes;

	public DatatypeRegistry() {
		this.datatypes = new HashMap<>();
	}

	public void registerFromResource(final String absoluteResourcePath) {
		try (InputStream ins = this.getClass().getResourceAsStream(absoluteResourcePath);) {
			final Reader reader = new InputStreamReader(ins);
			final JsonValue json = JsonValue.readHjson(reader);

			for (final JsonValue dt : json.asObject().get("datatypes").asArray()) {
				final String javaTypeName = dt.asObject().getString("javaType", null);
				if (null != javaTypeName) {
					final Class<?> cls = Class.forName(javaTypeName);
					final DatatypeInfoFromDefinition datatype = new DatatypeInfoFromDefinition(this, javaTypeName);
					this.datatypes.put(cls, datatype);

					final Map<String, JsonObject> jsonPropInfo = new HashMap<>();
					for (final JsonValue pi : dt.asObject().get("propertyInfo").asArray()) {
						final JsonObject jpi = pi.asObject();
						final String pname = jpi.getString("name", null);
						if (null == pname) {
							throw new DatatypeException("propertyInfo must define the name of a property", null);
						}
						jsonPropInfo.put(pname, jpi);
					}

					for (final Method m : cls.getMethods()) {
						if (DatatypeRegistry.isProperty(m)) {
							final DatatypeProperty pim = new DatatypeProperty(m);
							final JsonObject jpi = jsonPropInfo.get(pim.getName());
							if (null == jpi) {
								datatype.addPropertyInfo(pim.getName(), pim);
							} else {
								final boolean isIdentity = jpi.getBoolean("isIdentity", false);
								final boolean isComposite = jpi.getBoolean("isComposite", true);
								final boolean isReference = jpi.getBoolean("isReference", false);
								final int identityIndex = jpi.getInt("identityIndex", -1);
								final DatatypeProperty propInfo = new DatatypeProperty(m, pim.getName(), isIdentity, identityIndex, isComposite, isReference);
								datatype.addPropertyInfo(pim.getName(), propInfo);
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			throw new DatatypeException("Error trying to register datatypes from resource " + absoluteResourcePath, e);
		}

	}

	public DatatypeInfo getDatatypeInfo(final Class<?> class_) {
		DatatypeInfo dti = this.datatypes.get(class_);
		if (null == dti) {
			dti = new DatatypeInfoFromJavaClass(this, class_);
			this.datatypes.put(class_, dti);
		}
		return dti;
	}

}
