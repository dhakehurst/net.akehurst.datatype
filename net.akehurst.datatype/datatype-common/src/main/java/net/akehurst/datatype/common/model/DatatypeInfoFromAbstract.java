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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DatatypeInfoFromAbstract implements DatatypeInfo {

	protected final DatatypeRegistry registry;
	protected final Class<?> class_;
	private Map<String, DatatypeProperty> property_cache;
	private List<DatatypeProperty> propertyIdentity_cache;
	private Set<DatatypeProperty> propertyReference_cache;
	private Set<DatatypeProperty> propertyComposite_cache;

	public DatatypeInfoFromAbstract(final DatatypeRegistry registry, final Class<?> class_) {
		this.registry = registry;
		this.class_ = class_;
	}

	public abstract Set<DatatypeProperty> getDeclaredProperty();

	@Override
	public Map<String, DatatypeProperty> getAllProperty() {
		final Map<String, DatatypeProperty> allProps = new HashMap<>();
		if (null == this.class_) {

		} else {
			// TODO: handle overriden methods, i.e. don't include twice

			if (null != this.class_.getSuperclass()) {
				final DatatypeInfo di = this.registry.getDatatypeInfo(this.class_.getSuperclass());
				if (null != di) {
					final Map<String, DatatypeProperty> superclassMethods = di.getAllProperty();
					allProps.putAll(superclassMethods);
				}
			}
			for (final Class<?> intf : this.class_.getInterfaces()) {
				final DatatypeInfo di = this.registry.getDatatypeInfo(intf);
				if (null != di) {
					final Map<String, DatatypeProperty> interfaceMethods = di.getAllProperty();
					allProps.putAll(interfaceMethods);
				}
			}
			for (final DatatypeProperty dp : this.getDeclaredProperty()) {
				final DatatypeProperty sdp = allProps.get(dp.getName());
				// only override a property if this one is explicit or super is default
				if (null == sdp || !dp.isDefault() || sdp.isDefault()) {
					allProps.put(dp.getName(), dp);
				}
			}
		}
		return allProps;
	}

	@Override
	public Map<String, DatatypeProperty> getProperty() {
		if (null == this.property_cache) {
			this.property_cache = new HashMap<>();
			final Map<String, DatatypeProperty> p = this.getAllProperty();
			for (final DatatypeProperty dp : p.values()) {
				if (!dp.isIgnored()) {
					this.property_cache.put(dp.getName(), dp);
				}
			}
		}
		return this.property_cache;
	}

	@Override
	public List<DatatypeProperty> getPropertyIdentity() {
		if (null == this.propertyIdentity_cache) {
			this.propertyIdentity_cache = new ArrayList<>();
			for (final DatatypeProperty pi : this.getProperty().values()) {
				if (!pi.isIgnored() && pi.isIdentity()) {
					this.propertyIdentity_cache.add(pi);
				}
			}
			this.propertyIdentity_cache.sort((a, b) -> {
				if (a.getIdentityIndex() > b.getIdentityIndex()) {
					return 1;
				} else if (a.getIdentityIndex() < b.getIdentityIndex()) {
					return -1;
				} else {
					return 0;
				}
			});
		}
		return this.propertyIdentity_cache;
	}

	public Set<DatatypeProperty> getPropertyReference() {
		if (null == this.propertyReference_cache) {
			this.propertyReference_cache = new HashSet<>();
			for (final DatatypeProperty pi : this.getProperty().values()) {
				if (!pi.isIgnored() && pi.isReference()) {
					this.propertyReference_cache.add(pi);
				}
			}
		}
		return this.propertyReference_cache;
	}

	@Override
	public Set<DatatypeProperty> getPropertyComposite() {
		if (null == this.propertyComposite_cache) {
			this.propertyComposite_cache = new HashSet<>();
			for (final DatatypeProperty pi : this.getProperty().values()) {
				if (!pi.isIgnored() && !pi.isReference()) {
					this.propertyComposite_cache.add(pi);
				}
			}
		}
		return this.propertyComposite_cache;
	}

	@Override
	public String toString() {
		return this.class_.getName();
	}
}
