package com.xebia.axon.dependencies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileOutputStream;

/** arcs from 'source' to 'target' nodes */
public class Arc implements Comparable<Arc> {

	String name;
	Node source;
	Node target;

	static Map<Arc, Arc> allArcs = new HashMap<Arc, Arc>();

	private Arc() {
	}

	private Arc(String name, Node source, Node target) {

		if (name == null || source == null || target == null) {
			throw new IllegalArgumentException();
		}
		this.name = name;
		this.source = source;
		this.target = target;

		assert target != null;
		assert source != null;
		assert name != null;
	}

	public static Arc standAloneArc(String name, Node source, Node target) {
		Arc result = new Arc(name, source, target);
		return result;
	}

	public static Arc get(String name, Node source, Node target) {

		Arc lookup = new Arc(name, source, target);
		return allArcs.get(lookup);
	}

	public static Arc create(String name, Node source, Node target) {

		Arc result = new Arc(name, source, target);

		if (allArcs.containsKey(result)) {
			result = allArcs.get(result);
		} else {
			allArcs.put(result, result);
		}
		return result;
	}

	public static Arc findWsdl(Node source) {
		Arc result = null;
		for (Arc arc : allArcs.keySet()) {
			if ("wsdl".equals(arc.name) && arc.source.equals(source)) {
				result = arc;
				break;
			}
		}

		if (result == null) {
			for (Arc arc : allArcs.keySet()) {
				if ("interface".equals(arc.name) && arc.source.equals(source)) {
					result = findWsdl(arc.target);
					if (result != null) {
						break;
					}
				}
			}
		}
		return result;
	}

	public String toString(boolean withLabel) {
		String s = source.name != null ? source.name.substring(source.name
				.lastIndexOf('/') + 1) : "null";
		String t = target.name != null ? target.name.substring(target.name
				.lastIndexOf('/') + 1) : "null";
		String result = "\"" + s + "\" -> \"" + t + "\"";
		if (withLabel) {
			result = result + "[label=\"" + name + "\"];";
		}
		return result;
	}

	public String toString() {
		return toString(true);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Arc other = (Arc) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	public int compareTo(Arc other) {
		int result;

		if (source.name != null && other.name != null) {
			result = source.name.compareTo(other.source.name);
		} else {
			throw new IllegalArgumentException("compare failed on : " + this);
		}
		if (result == 0) {
			if (target.name != null && other.target.name != null) {
				result = target.name.compareTo(other.target.name);
			} else {
				throw new IllegalArgumentException("compare failed on : "
						+ this);
			}
			if (result == 0) {
				result = name.compareTo(other.name);
			}
		}
		return result;
	}

	public static Set<Arc> findAllArcsForNode(Set<Arc> allArcs, Set<Arc> arcs,
			Node node) {
		if (node != null) {
			for (Arc arc : allArcs) {
				if (arc.source.equals(node) && !arcs.contains(arc)) {
					arcs.add(arc);
					findAllArcsForNode(allArcs, arcs, arc.target);
				}
			}
		}
		return arcs;
	}

	/** Find all root nodes */
	public static Set<Node> findAllRootNodes(Set<Arc> arcs) {
		boolean inSet;
		boolean root;
		Set<Node> result = new HashSet<Node>();
		for (Node node : Node.allNodes.values()) {
			inSet = false;
			root = true;
			for (Arc arc : arcs) {
				if (arc.source.equals(node)) {
					inSet = true;
				}
				if (arc.target.equals(node)) {
					root = false;
				}
			}
			if (inSet && root) {
				result.add(node);
			}
		}
		return result;
	}

	/** Find all leaf nodes */
	public static Set<Node> findAllLeafNodes(Set<Arc> arcs) {
		boolean inSet;
		boolean leaf;
		Set<Node> result = new HashSet<Node>();
		for (Node node : Node.allNodes.values()) {
			inSet = false;
			leaf = true;
			for (Arc arc : arcs) {
				if (arc.source.equals(node) || arc.target.equals(node)) {
					inSet = true;
				}
				if (arc.source.equals(node)) {
					leaf = false;
				}
			}
			if (inSet && leaf) {
				result.add(node);
			}
		}
		return result;
	}

	/** Remove all arcs between root and leaves */
	public static Set<Arc> createRootAndLeafGraph(Node root, Set<Arc> arcs) {
		Set<Arc> result = new HashSet<Arc>();
		Set<Node> leafs = findAllLeafNodes(arcs);

		for (Node leaf : leafs) {
			result.add(Arc.standAloneArc("depends", root, leaf));
		}
		return result;
	}

	/** Remove all constructor relations */
	public static Set<Arc> removeAllInitArcs(Set<Arc> arcs) {
		Set<Arc> exclude = new HashSet<Arc>();
		for (Arc arc : arcs) {
			if ("<init>".equals(arc.name)) {
				exclude.add(arc);
			}
		}
		System.out.println("removing " + exclude.size() + " arcs.");
		System.out.println(exclude);
		System.out.println("original set has " + arcs.size() + " arcs.");
		arcs.removeAll(exclude);
		System.out.println("original set now has " + arcs.size()
				+ " arcs left.");

		return arcs;
	}

	public static Set<Arc> findAllArcsForClient(Set<Arc> arcs, Node node) {
		Set<Arc> result = new HashSet<Arc>();
		Node source = null;
		for (Arc arc : arcs) {
			if (node.equals(arc.source)) {
				source = arc.source;
				break;
			}
		}

		findAllArcsForNode(arcs, result, source);
		return result;
	}

	/** Find all arcs with 'name' */
	public static Set<Arc> findAllArcsWithName(Set<Arc> arcs, String name) {
		Set<Arc> result = new HashSet<Arc>();
		for (Arc arc : Arc.allArcs.keySet()) {
			if (arc.name.equals(name)) {
				findAllArcsForNode(Arc.allArcs.keySet(), result, arc.source);
			}
		}

		return result;
	}



	public static void printAll(Set<Arc> arcs, String name) {
		printAll(arcs, name, true);
	}
	
	public static Set<Node> getAllNodes(Set<Arc> arcs) {
		Set<Node> result = new TreeSet<Node>();

		// Add all nodes for formatting
		arcs.stream().forEach(a -> {
			result.add(a.source);
			result.add(a.target);
		});

		return result;
	}	
	
	public static SortedSet<Arc> sort(Set<Arc> arcs) {
		SortedSet<Arc> result = new TreeSet<Arc>();
		result.addAll(arcs);
		return result;
	}

	public static void printAll(Set<Arc> arcs, String name, boolean withLabel) {
		File dotfile = new File(name);

		Set<Arc> ordered = sort(arcs);
		Set<Node> nodes = getAllNodes(arcs);

		File dir = new File("graphs");
		File f = new File(dir, dotfile.getName() + ".dot");
		FileOutputStream fos = null;
		try {
			dir.mkdirs();
			fos = new FileOutputStream(f);
			final PrintWriter writer = new PrintWriter(fos);
			writer.println("digraph " + dotfile.getName() + " {");
			nodes.stream().forEach(node -> writer.print(node.getDotStyle()));
			ordered.stream().forEach(
					arc -> writer.println(arc.toString(withLabel)));
			writer.println("}");

			writer.close();
			fos.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static long numberOfIncomingArcs(Set<Arc> arcs, Node node) {
		return arcs.stream().filter(arc -> node.equals(arc.target)).count();
	}

	public static long numberOfOutgoingArcs(Set<Arc> arcs, Node node) {
		return arcs.stream().filter(arc -> node.equals(arc.source)).count();
	}

	public static long totalNumberOfArcs(Set<Arc> arcs, Node node) {
		return arcs.stream().filter(arc -> node.equals(arc.source) || node.equals(arc.target)).count();
	}

	public static void printNodeStatistics(Set<Arc> arcs, String name) {
		File dotfile = new File(name);
		Set<Node> nodes = getAllNodes(arcs);

		File dir = new File("graphs");
		File f = new File(dir, dotfile.getName() + "-statistics.txt");
		FileOutputStream fos = null;
		try {
			dir.mkdirs();
			fos = new FileOutputStream(f);
			final PrintWriter writer = new PrintWriter(fos);
			writer.println("#node\tincoming\toutgoing\ttotal");
			for (Node node : nodes) {
				writer.println(String.format("%s\t%d\t%d\t%d", node.name,
						numberOfIncomingArcs(arcs, node),
						numberOfOutgoingArcs(arcs, node),
						totalNumberOfArcs(arcs, node)));
			}
			writer.println("}");
			writer.close();
			fos.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
