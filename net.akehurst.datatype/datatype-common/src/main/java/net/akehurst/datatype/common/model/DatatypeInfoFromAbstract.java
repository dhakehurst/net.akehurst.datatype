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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DatatypeInfoFromAbstract implements DatatypeInfo {

	protected final DatatypeRegistry registry;
	private List<DatatypeProperty> propertyIdentity_cache;
	private Set<DatatypeProperty> propertyReference_cache;
	private Set<DatatypeProperty> propertyComposite_cache;

	public DatatypeInfoFromAbstract(final DatatypeRegistry registry) {
		this.registry = registry;
	}

	@Override
	public abstract Set<DatatypeProperty> getProperty();

	@Override
	public List<DatatypeProperty> getPropertyIdentity() {
		if (null == this.propertyIdentity_cache) {
			this.propertyIdentity_cache = new ArrayList<>();
			for (final DatatypeProperty pi : this.getProperty()) {
				if (pi.isIdentity()) {
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
			for (final DatatypeProperty pi : this.getProperty()) {
				if (pi.isReference()) {
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
			for (final DatatypeProperty pi : this.getProperty()) {
				if (pi.isComposite()) {
					this.propertyComposite_cache.add(pi);
				}
			}
		}
		return this.propertyComposite_cache;
	}

}
