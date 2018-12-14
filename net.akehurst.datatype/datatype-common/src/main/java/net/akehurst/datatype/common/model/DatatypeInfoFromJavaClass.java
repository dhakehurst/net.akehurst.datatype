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
import java.util.HashSet;
import java.util.Set;

public class DatatypeInfoFromJavaClass extends DatatypeInfoFromAbstract implements DatatypeInfo {

	private final Class<?> class_;
	private Set<DatatypeProperty> property_cache;

	public DatatypeInfoFromJavaClass(final DatatypeRegistry registry, final Class<?> class_) {
		super(registry);
		this.class_ = class_;
	}

	public String getPackageName() {
		return this.class_.getPackage().getName();
	}

	public String getName() {
		return this.class_.getSimpleName();
	}

	@Override
	public Set<DatatypeProperty> getProperty() {
		if (null == this.property_cache) {
			if (null == this.class_) {
				this.property_cache = new HashSet<>();
			} else {
				// TODO: handle overriden methods, i.e. don't include twice
				this.property_cache = new HashSet<>();
				if (null != this.class_.getSuperclass()) {
					final Set<DatatypeProperty> superclassMethods = this.registry.getDatatypeInfo(this.class_.getSuperclass()).getProperty();
					this.property_cache.addAll(superclassMethods);
				}
				for (final Class<?> intf : this.class_.getInterfaces()) {
					final Set<DatatypeProperty> interfaceMethods = this.registry.getDatatypeInfo(intf).getProperty();
					this.property_cache.addAll(interfaceMethods);
				}
				for (final Method m : this.class_.getDeclaredMethods()) {
					if (this.registry.isProperty(m)) {
						final DatatypeProperty dp = new DatatypeProperty(m);
						this.property_cache.add(dp);
					}
				}
			}
		}
		return this.property_cache;
	}

}
