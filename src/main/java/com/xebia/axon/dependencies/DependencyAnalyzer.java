/***
 * based upon a ASM examples: examples showing how ASM can be used
 */
package com.xebia.axon.dependencies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.ArchiveException;
import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

/**
 * Analyses the Axon application for dependencies between Events, Commands,
 * Entities, AggregateRoots and Handlers. The dependency graph is based upon
 * heuristic interpretation of the byte code.
 */
public class DependencyAnalyzer {

	public static void main(final String[] args) throws IOException {
		analyzeArchives(args);

		// one graph with all dependencies.
		Arc.printAll(Arc.allArcs.keySet(), "all");
		Arc.printNodeStatistics(Arc.allArcs.keySet(), "all");
		Arc.printEventUsage(Arc.allArcs.keySet(), "all");

		// a graph for each Entity, Saga and aggregate root.
		for (String name : Node.allNodes.keySet()) {
			Node node = Node.get(name);
			if (name.endsWith("Entity") || name.endsWith("Saga") || node.isCommandType() || node.isAggregateRoot()) {
				Set<Arc> arcs = Arc.findAllArcsForClient(Arc.allArcs.keySet(), node);

				Arc.printAll(arcs, name);
			}
		}
	}

	private static void analyzeArchives(final String[] args) throws FileNotFoundException, IOException, ArchiveException {
		int size = 0;
		int pass = 0;

		do {
			pass++;
			size = Node.allNodes.size();

			File.setDefaultArchiveDetector(new DefaultArchiveDetector(ArchiveDetector.ALL, new Object[] { "dar",
					new de.schlichtherle.io.archive.zip.JarDriver() }));

			ClassVisitor v = new AxonClassVisitor();

			long l1 = System.currentTimeMillis();
			for (int i = 0; i < args.length; i++) {

				File file = new File(args[i]);
				System.err.println("opening " + args[i]);
				processClassFile(file, v);
			}
			long l2 = System.currentTimeMillis();
			System.err.println("pass " + pass + ", added: " + (Node.allNodes.size() - size) + ", time: " + (l2 - l1) / 1000f);
			File.umount();
		} while (Node.allNodes.size() > size);
	}

	private static void processClassFile(File file, ClassVisitor v) throws FileNotFoundException, IOException {

		if (file.isFile() && file.getName().endsWith(".class")) {
			FileInputStream ios = new FileInputStream(file);
			try {
				new ClassReader(ios).accept(v, 0);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println(String.format("Could not read %s", file.getCanonicalPath()));
			}
			ios.close();
		} else if (file.isDirectory() || file.isArchive()) {
			File[] files = file.listFiles(File.getDefaultArchiveDetector());
			for (int i = 0; files != null && i < files.length; i++) {
				processClassFile(files[i], v);
			}
		}

	}
}
