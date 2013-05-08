/**
 * Copyright 2013 Julien Silland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.turn.shapeshifter;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class stores a representation of the references between protobuf
 * message descriptors and implements Tarjan's algorithm to detect loops,
 * which are forbidden in {@link AutoSchema} instances.
 *
 * @author Julien Silland (julien@soliton.io)
 * @see <a href="http://en.wikipedia.org/wiki/Tarjan%E2%80%99s_strongly_connected_components_algorithm">Tarjan's algorithm</a>
 */
class ProtoDescriptorGraph {
	
	private final ImmutableMultimap<Descriptor, Descriptor> graph;
	
	private ProtoDescriptorGraph(ImmutableMultimap<Descriptor, Descriptor> graph) {
		this.graph = graph;
	}
	
	public static ProtoDescriptorGraph of(Descriptor descriptor) {
		Multimap<Descriptor, Descriptor> graphBuilder = HashMultimap.create();
		populate(descriptor, graphBuilder);
		return new ProtoDescriptorGraph(
				ImmutableMultimap.<Descriptor, Descriptor>builder().putAll(graphBuilder).build());
	}
	
	private static void populate(Descriptor descriptor,
			Multimap<Descriptor, Descriptor> graphBuilder) {
		if (graphBuilder.containsKey(descriptor)) {
			return;
		}
		
		for (FieldDescriptor field : descriptor.getFields()) {
			if (Type.MESSAGE.equals(field.getType())) {
				Descriptors.Descriptor subDescriptor = field.getMessageType();
				graphBuilder.put(descriptor, subDescriptor);
				populate(subDescriptor, graphBuilder);
			}	
		}		
	}
	
	public boolean isLooping() {
		Map<Descriptor, Link> links = Maps.newHashMap();
		for (Descriptor descriptor : graph.keySet()) {
			links.put(descriptor, new Link());
			for (Descriptor child : graph.get(descriptor)) {
				if (child.equals(descriptor)) {
					return true;
				}
				if (!links.containsKey(child)) {
					links.put(child, new Link());
				}
			}
		}
		
		Stack<Descriptor> stack = new Stack<Descriptor>();
		List<List<Descriptor>> sccs = Lists.newArrayList();
		
		AtomicInteger index = new AtomicInteger(0);
		for (Descriptor descriptor : graph.keySet()) {
			if (links.get(descriptor).index == -1) {
				strongConnect(descriptor, index, links, stack, sccs);
			}
		}

		return !sccs.isEmpty();
	}
	
	private void strongConnect(Descriptor descriptor, AtomicInteger index,
			Map<Descriptor, Link> links, Stack<Descriptor> stack, List<List<Descriptor>> sccs) {
		links.get(descriptor).index = index.get();
		links.get(descriptor).lowLink = index.get();
		index.incrementAndGet();
		stack.push(descriptor);
		
		for (Descriptor child : graph.get(descriptor)) {
			if (links.get(child).index == -1) {
				strongConnect(child, index, links, stack, sccs);
				links.get(descriptor).lowLink = Math.min(
						links.get(descriptor).lowLink, links.get(child).lowLink);
			} else if (stack.contains(child)) {
				links.get(descriptor).lowLink = Math.min(
						links.get(descriptor).lowLink, links.get(child).index);
			}
		}
		
		if (links.get(descriptor).index == links.get(descriptor).lowLink) {
			List<Descriptor> scc = Lists.newArrayList();
			Descriptor component = null;
			do {
				component = stack.pop();
				scc.add(component);
			} while (!component.equals(descriptor));
			if (scc.size() != 1) {
				sccs.add(scc);
			}
		}
	}
	
	@Override
	public String toString() {
		Multimap<String, String> namedGraph = HashMultimap.create();
		for (Map.Entry<Descriptor, Descriptor> entry : graph.entries()) {
			namedGraph.put(entry.getKey().getName(), entry.getValue().getName());
		}
		return namedGraph.toString();
	}
	
	private static final class Link {
		
		int index = -1;
		int lowLink = 0;
		
	}
}
