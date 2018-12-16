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
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.api.DatatypeException;

public class DatatypeRegistry {

	private final Map<Class<?>, DatatypeInfo> datatypes;

	public DatatypeRegistry() {
		this.datatypes = new HashMap<>();
	}

	public void registerFromResource(final InputStream ins) {
		try {
			final Reader reader = new InputStreamReader(ins);
			final JsonValue json = JsonValue.readHjson(reader);

			for (final JsonValue dt : json.asObject().get("datatypes").asArray()) {
				final String javaTypeName = dt.asObject().getString("javaType", null);
				if (null != javaTypeName) {
					final Class<?> cls = Class.forName(javaTypeName);
					if (null == cls) {
						throw new DatatypeException("class not found for " + javaTypeName, null);
					}
					final DatatypeInfoFromDefinition datatype = new DatatypeInfoFromDefinition(this, cls);
					this.datatypes.put(cls, datatype);

					final Map<String, JsonObject> jsonPropInfo = new HashMap<>();
					if (null != dt.asObject().get("propertyInfo")) {
						for (final JsonValue pi : dt.asObject().get("propertyInfo").asArray()) {
							final JsonObject jpi = pi.asObject();
							final String pname = jpi.getString("name", null);
							if (null == pname) {
								throw new DatatypeException("propertyInfo must define the name of a property", null);
							}
							jsonPropInfo.put(pname, jpi);
						}
					}
					for (final Method m : cls.getDeclaredMethods()) {
						if (this.isProperty(m)) {
							final DatatypeProperty pim = new DatatypeProperty(m);
							final JsonObject jpi = jsonPropInfo.get(pim.getName());
							if (null == jpi) {
								datatype.addPropertyInfo(pim.getName(), pim);
							} else {
								final boolean isIdentity = jpi.getBoolean("isIdentity", false);
								final int identityIndex = jpi.getInt("identityIndex", -1);
								final boolean isReference = jpi.getBoolean("isReference", false);
								final boolean ignored = jpi.getBoolean("ignored", false);
								final DatatypeProperty propInfo = new DatatypeProperty(m, pim.getName(), ignored, isIdentity, identityIndex, isReference);
								datatype.addPropertyInfo(pim.getName(), propInfo);
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			throw new DatatypeException("Error trying to register datatypes from resource " + ins, e);
		}

	}

	public DatatypeInfo getDatatypeInfo(final Class<?> class_) {
		if (null == class_ || Object.class == class_) {
			return null;
		}
		DatatypeInfo dti = this.datatypes.get(class_);
		if (null == dti) {
			dti = new DatatypeInfoFromJavaClass(this, class_);
			this.datatypes.put(class_, dti);

		}
		return dti;
	}

	public boolean isDatatype(final Class<?> class_) {
		if (null == class_ || Object.class == class_) {
			return false;
		}
		if (!this.datatypes.containsKey(class_) && null == class_.getAnnotation(Datatype.class)) {
			// check interfaces, superclasses are included because @Datatype is marked as @Inherited
			for (final Class<?> intf : class_.getInterfaces()) {
				if (this.isDatatype(intf)) {
					return true;
				}
			}
			// check interfaces of superclass
			return this.isDatatype(class_.getSuperclass());
		} else {
			return true;
		}
	}

	public boolean isDeclaredDatatype(final Class<?> cls) {
		if (null == cls || Object.class == cls) {
			return false;
		}
		if (!this.datatypes.containsKey(cls) && null == cls.getAnnotation(Datatype.class)) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isProperty(final Method m) {
		if (null == m) {
			return false;
		}
		if (null != m.getDeclaredAnnotation(Query.class)) {
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

}
