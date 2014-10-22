package com.xebia.axon.dependencies;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Node implements Comparable<Node> {
	String name;
	static Map<String, Node> allNodes = new HashMap<String, Node>();
	Properties properties = new Properties();

	private Node() {
	}

	public static Node get(String name) {
		return allNodes.get(name);
	}

	public String getShortName() {
		return name != null ? name.substring(name.lastIndexOf('/') + 1) : "null";
	}

	public static Node create(String name) {
		Node result = get(name);

		if (result == null) {
			result = new Node();
			result.name = name;
			allNodes.put(name, result);
		}
		return result;
	}

	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Node other = (Node) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public boolean isEventType() {
		return properties.containsKey("Event");
	}

	public void setEventType() {
		properties.put("Event", true);
	}

	public boolean isCommandType() {
		return properties.containsKey("Command");
	}

	public void setCommandType() {
		properties.put("Command", true);
	}

	public boolean isAggregateRoot() {
		return properties.containsKey("isAggregateRoot");
	}

	public void setAggregateRoot() {
		properties.put("isAggregateRoot", true);
	}

	public boolean isSaga() {
		return properties.containsKey("isSaga");
	}

	public void setSaga() {
		properties.put("isSaga", true);
	}
	
	public boolean isEventHandler() {		
		return properties.containsKey("isEventHandler");
	}

	public void setEventHandler() {
		properties.put("isEventHandler", true);
	}
	
	public boolean isCommandHandler() {
		return properties.containsKey("isCommandHandler");
	}

	public void setCommandHandler() {
		properties.put("isCommandHandler", true);
	}
	
	public String getDotStyle() {
		if (isEventType()) {
			return String.format("%s [ shape=parallelogram];\n", getShortName());
		}
		if (isCommandType()) {
			return String.format("%s [ shape=cds ];\n", getShortName());
		}
		if (isAggregateRoot()) {
			return String.format("%s [ shape=box3d fillcolor=red style=filled fontcolor=white  ];\n", getShortName());
		}
		if (name.endsWith("Entity")) {
			return String.format("%s [ shape=component fillcolor=orange style=filled ];\n", getShortName());
		}
		if (name.endsWith("Saga")) {
			return String.format("%s [ shape=octagon fillcolor=lightblue style=filled  ];\n", getShortName());
		}
		return "";
	}

	@Override
	public int compareTo(Node o) {
		return name.compareTo(o.name);
	}	
}
