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

package net.akehurst.datatype.transform.hjson.rule;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import net.akehurst.datatype.api.DatatypeException;
import net.akehurst.datatype.common.model.DatatypeInfo;
import net.akehurst.datatype.common.model.DatatypeProperty;
import net.akehurst.datatype.common.model.DatatypeRegistry;
import net.akehurst.datatype.transform.hjson.HJsonTransformerDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;
import net.akehurst.transform.binary.api.TransformException;

/**
 * The class for the LHS objects must be annotated with @Datatype. Constructor arguments for the LHS object must correspond to an accessor annotated
 * with @Identity, declared in the corresponding order to the constructor arguments. The RHS object will contain an additional member named "_class" that
 * contains the full class name of the transformed LHS object.
 *
 */
public class Datatype2HJsonObject extends Object2JsonValue<Object, JsonObject> implements BinaryRule<Object, JsonObject> {

	// should get set in ... before it is used
	private DatatypeRegistry registry;

	private List<String> createPath(final Object from, final Object to) {
		if (null == from) {
			return null;
		} else if (Objects.equals(from, to)) {
			return new ArrayList<>();
		} else {
			if (from instanceof Collection<?>) {
				final Collection<?> arr = (Collection<?>) from;
				int i = 0;
				for (final Object o : arr) {
					final List<String> path = this.createPath(o, to);
					if (null != path) {
						path.add(0, Integer.toString(i));
						return path;
					}
					++i;
				}
				return null;
			} else if (this.registry.isDatatype(from.getClass())) { // treat from as an Object
				final DatatypeInfo datatype = this.registry.getDatatypeInfo(from.getClass());
				for (final DatatypeProperty pi : datatype.getPropertyComposite()) {
					if (!pi.isReference()) {
						final Object value = pi.getValueFrom(from);
						final List<String> path = this.createPath(value, to);
						if (null != path) {
							path.add(0, pi.getName());
							return path;
						}
					}
				}
				return null;
			} else {
				return null;
			}
		}
	}

	private JsonObject getReferenceTo(final Object referedToObject, final BinaryTransformer transformer) {
		if (null == referedToObject) {
			return null;
		}
		final HJsonTransformerDefault hjt = (HJsonTransformerDefault) transformer;
		try {
			final List<String> path = this.createPath(hjt.getJavaRoot(), referedToObject);
			if (null == path) {
				final JsonObject reference = new JsonObject();
				final String refStr = "<Unknown reference>";
				reference.add("$type", "Reference");
				reference.add("$ref", refStr);
				return reference;
			} else {
				final JsonObject reference = new JsonObject();
				final String refStr = "#/" + Seq.seq(path).toString("/");
				reference.add("$type", "Reference");
				reference.add("$ref", refStr);
				return reference;
			}
		} catch (final StackOverflowError e) {
			throw new DatatypeException("Did you forget to mark something as a reference? Error creating reference to " + referedToObject, e);
		}
	}

	private JsonValue resolveReference(final List<String> path, final JsonValue from) {
		if (path.isEmpty()) {
			return from;
		} else {
			final Tuple2<Optional<String>, Seq<String>> t = Seq.seq(path).splitAtHead();
			final String head = t.v1().get();
			final List<String> tail = t.v2().toList();

			if (from.isArray()) {
				final int index = Integer.parseInt(head);
				final JsonValue v = from.asArray().get(index);
				return this.resolveReference(tail, v);
			} else if (from.isObject()) {
				final JsonObject jo = from.asObject();
				final String type = null == jo.get("$type") ? null : jo.get("$type").asString();
				if (null != type && Objects.equals("Set", type)) {
					return this.resolveReference(path, jo.get("$elements"));
				} else if (null != type && Objects.equals("List", type)) {
					return this.resolveReference(path, jo.get("$elements"));
				} else if (null != type && Objects.equals("Map", type)) {
					throw new UnsupportedOperationException(); // TODO:
				} else if (null != type && Objects.equals("Enum", type)) {
					return null;
				} else if (null != type && Objects.equals("Reference", type)) {
					return null;
				} else {
					final JsonValue v = from.asObject().get(head);
					return this.resolveReference(tail, v);
				}
			} else {
				return null;
			}
		}
	}

	private JsonValue resolveReference(final JsonObject referenceObject, final BinaryTransformer transformer) {
		if (null != referenceObject.get("$ref")) {
			final String pathStr = referenceObject.get("$ref").asString();
			if (pathStr.startsWith("#/")) {
				final String pathStr2 = pathStr.substring(2);
				final List<String> path = pathStr2.isEmpty() ? Arrays.asList() : Arrays.asList(pathStr2.split("/"));
				final HJsonTransformerDefault hjt = (HJsonTransformerDefault) transformer;
				return this.resolveReference(path, hjt.getHJsonRoot());
			} else {
				// throw new TransformException("$ref is not a valid Json Path expression: " + pathStr, null);
				// TODO: need to log a warning really!
				return null;
			}
		} else {
			throw new TransformException("JsonObject is not a reference: " + referenceObject.toString(), null);
		}
	}

	private void setValueRight2Left(final Object left, final DatatypeProperty pi, final JsonValue rightValue, final BinaryTransformer transformer) {
		if (List.class.isAssignableFrom(pi.getType())) {
			final List leftValue = transformer.transformRight2Left((Class<BinaryRule<List, JsonValue>>) (Object) List2JsonArray.class, rightValue);
			pi.setValueFor(left, leftValue);
		} else if (Set.class.isAssignableFrom(pi.getType())) {
			final Set leftValue = transformer.transformRight2Left((Class<BinaryRule<Set, JsonValue>>) (Object) Set2JsonArray.class, rightValue);
			pi.setValueFor(left, leftValue);

		} else if (Map.class.isAssignableFrom(pi.getType())) {
			final Map leftValue = transformer.transformRight2Left((Class<BinaryRule<Map, JsonObject>>) (Object) Map2JsonObject.class, rightValue.asObject());
			pi.setValueFor(left, leftValue);
		} else {
			final Object leftValue = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, rightValue);
			pi.setValueFor(left, leftValue);
		}
	}

	private void setRegistry(final BinaryTransformer transformer) {
		final HJsonTransformerDefault trans = (HJsonTransformerDefault) transformer;
		this.registry = trans.getDatatypeRegistry();
	}

	private DatatypeInfo getDatatypeInfo(final Class<?> class_) {
		return this.registry.getDatatypeInfo(class_);
	}

	private JsonValue createContainerOfReferences(final BinaryTransformer transformer, final DatatypeProperty pi, final Object container) {
		if (Set.class.isAssignableFrom(pi.getType())) {
			final Set<?> left = (Set<?>) container;
			final JsonObject right = new JsonObject();
			right.add("$type", "Set");
			final JsonArray elements = new JsonArray();
			right.add("$elements", elements);
			for (final Object elem : left) {
				final JsonObject reference = this.getReferenceTo(elem, transformer);
				if (null != reference) {
					right.add(pi.getName(), reference);
				}
			}
			return right;
		}
		if (List.class.isAssignableFrom(pi.getType())) {
			final Set<?> left = (Set<?>) container;
			final JsonObject right = new JsonObject();
			right.add("$type", "List");
			final JsonArray elements = new JsonArray();
			right.add("$elements", elements);
			for (final Object elem : left) {
				final JsonObject reference = this.getReferenceTo(elem, transformer);
				if (null != reference) {
					right.add(pi.getName(), reference);
				}
			}
			return right;
		} else {
			throw new DatatypeException("Unsupported container type of references " + pi.getType(), null);
		}
	}

	@Override
	public boolean isValidForLeft2Right(final Object left, final BinaryTransformer transformer) {
		if (null == left) {
			return false;
		}
		this.setRegistry(transformer);
		return this.registry.isDatatype(left.getClass());
	}

	@Override
	public boolean isValidForRight2Left(final JsonObject right, final BinaryTransformer transformer) {
		if (null == right) {
			return false;
		}
		return null != right.get("$class");
	}

	@Override
	public boolean isAMatch(final Object left, final JsonObject right, final BinaryTransformer transformer) {
		final String n_left = left.getClass().getName();
		final String n_right = right.getString("$class", "<Undefined>"); // should never be undefined due to isValid check
		return Objects.equals(n_left, n_right);
	}

	@Override
	public JsonObject constructLeft2Right(final Object left, final BinaryTransformer transformer) {
		this.setRegistry(transformer);
		final JsonObject right = new JsonObject();
		right.add("$class", left.getClass().getName());

		final DatatypeInfo datatype = this.getDatatypeInfo(left.getClass());
		for (final DatatypeProperty pi : datatype.getPropertyIdentity()) {
			final Object value = pi.getValueFrom(left); // RT.wrap(() -> m.invoke(left));
			final JsonValue memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, value);
			if (pi.isReference()) {
				final JsonObject reference = this.getReferenceTo(value, transformer);
				if (null != reference) {
					right.add(pi.getName(), reference);
				}
			} else {
				right.add(pi.getName(), memberValue);
			}
		}

		return right;
	}

	@Override
	public Object constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
		this.setRegistry(transformer);
		final String className = right.getString("$class", "<Undefined>"); // should never be undefined due to isValid check

		final Class<?> leftClass = RT.wrap(() -> Class.forName(className));
		final DatatypeInfo datatype = this.getDatatypeInfo(leftClass);

		final List<Class<?>> parameterTypes = new ArrayList<>();
		final List<Object> initargs = new ArrayList<>();

		for (final DatatypeProperty pi : datatype.getPropertyIdentity()) {
			parameterTypes.add(pi.getType());
			final JsonValue mv = right.get(pi.getName());
			if (!pi.isReference()) { // not a reference
				final Object v = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, mv);
				initargs.add(v);
			} else {
				if (null != mv) {
					final JsonValue rv = this.resolveReference(mv.asObject(), transformer);
					final Object v = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, rv);
					initargs.add(v);
				} else {
					// use null value for reference
					initargs.add(null);
				}
			}
		}

		final Object left = RT.wrap(() -> {
			final Constructor<?> cons = Class.forName(className).getConstructor(parameterTypes.toArray(new Class<?>[parameterTypes.size()]));
			return cons.newInstance(initargs.toArray(new Object[initargs.size()]));
		});

		return left;
	}

	@Override
	public void updateLeft2Right(final Object left, final JsonObject right, final BinaryTransformer transformer) {
		this.setRegistry(transformer);
		final DatatypeInfo datatype = this.getDatatypeInfo(left.getClass());
		for (final DatatypeProperty pi : datatype.getProperty().values()) {
			final Object value = pi.getValueFrom(left);
			boolean includeIt = null != value;
			if (value instanceof Collection && ((Collection) value).isEmpty()) {
				includeIt = false;
			}
			if (includeIt) {
				if (pi.isReference()) {
					if (pi.isContainer()) {
						final JsonValue rightContainer = this.createContainerOfReferences(transformer, pi, value);
						right.add(pi.getName(), rightContainer);
					} else {
						final JsonObject reference = this.getReferenceTo(value, transformer);
						if (null != reference) {
							right.add(pi.getName(), reference);
						}
					}
				} else {
					final JsonValue memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, value);
					right.add(pi.getName(), memberValue);
				}
			}
		}
	}

	@Override
	public void updateRight2Left(final Object left, final JsonObject right, final BinaryTransformer transformer) {
		this.setRegistry(transformer);
		final DatatypeInfo datatype = this.getDatatypeInfo(left.getClass());
		for (final DatatypeProperty pi : datatype.getProperty().values()) {
			if (pi.isIdentity()) {
				// should have been set during construction
			} else {
				final JsonValue memberValue = right.get(pi.getName());
				if (null != memberValue) {
					if (pi.isReference()) {
						final JsonValue rv = this.resolveReference(memberValue.asObject(), transformer);
						this.setValueRight2Left(left, pi, rv, transformer);
					} else {
						this.setValueRight2Left(left, pi, memberValue, transformer);
					}
				}
			}
		}
	}

}
