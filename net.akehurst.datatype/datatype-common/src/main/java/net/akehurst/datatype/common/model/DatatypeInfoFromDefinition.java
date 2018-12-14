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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DatatypeInfoFromDefinition extends DatatypeInfoFromAbstract implements DatatypeInfo {

	private final String fullClassName;
	private final Map<String, DatatypeProperty> propertyInfo;

	public DatatypeInfoFromDefinition(final DatatypeRegistry registry, final String fullClassName) {
		super(registry);
		this.fullClassName = fullClassName;
		this.propertyInfo = new HashMap<>();
	}

	@Override
	public Set<DatatypeProperty> getProperty() {
		return new HashSet<>(this.propertyInfo.values());
	}

	public void addPropertyInfo(final String pname, final DatatypeProperty propInfo) {
		this.propertyInfo.put(pname, propInfo);
	}

}
