package com.xebia.axon.dependencies;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * analyzing java classes for Axon Framework dependencies.
 * 
 * @author mark
 */
public class AxonClassVisitor extends ClassVisitor {
	String className;

	public AxonClassVisitor() {
		super(Opcodes.ASM5);

	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = name;
		if (superName.equals("org/axonframework/eventsourcing/annotation/AbstractAnnotatedAggregateRoot")) {
			Node node = Node.create(className);
			node.setAggregateRoot();
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean visibleAtRuntime) {
		return super.visitAnnotation(name, visibleAtRuntime);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodVisitor result = new AxonMethodVisitor(className, access, name, desc, signature, exceptions);
		return result;
	}

}
