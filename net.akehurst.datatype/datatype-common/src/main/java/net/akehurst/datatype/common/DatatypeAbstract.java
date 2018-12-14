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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.akehurst.datatype.annotation.Query;

/**
 * A common base class for datatypes that can be identified by a few constructor arguments.
 *
 * <p>
 * The identityValues passed as arguments should never change, and are ideally primitive types.
 *
 */
public abstract class DatatypeAbstract {

	public DatatypeAbstract(final Object... identityValues) {
		this.identityValues = identityValues;
		this.hashCode_cache = Objects.hash(this.identityValues);
	}

	private final Object[] identityValues;

	@Query
	protected List<Object> getIdentityValues() {
		return Collections.unmodifiableList(Arrays.asList(this.identityValues));
	}

	int hashCode_cache;

	@Override
	public int hashCode() {
		return this.hashCode_cache;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this.getClass().isInstance(obj)) {
			final DatatypeAbstract other = (DatatypeAbstract) obj;
			return Arrays.equals(this.identityValues, other.identityValues);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append(this.getClass().getSimpleName());
		b.append("{");
		for (int i = 0; i < this.identityValues.length; ++i) {
			final Object o = this.identityValues[i];
			b.append(o.toString());
			if (i > this.identityValues.length - 2) {
				b.append(",");
			}
		}
		b.append(Arrays.asList(this.identityValues));
		b.append("}");
		return b.toString();
	}

}
