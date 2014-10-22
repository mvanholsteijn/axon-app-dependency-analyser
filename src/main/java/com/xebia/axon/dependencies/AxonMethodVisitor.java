package com.xebia.axon.dependencies;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * responsible for analyzing java method implementation for Axon Framework dependencies.
 * 
 * @author mark
 */
class AxonMethodVisitor extends MethodVisitor {

	String className;
	boolean isEventHandler;
	boolean isCommandHandler;
	boolean isSagaHandler;
	boolean publishInvoked;
	boolean sendInvoked;

	int access;
	String methodName, desc, signature;
	String[] exceptions;

	Set<Node> commandsAndEventsInstantiated = new HashSet<Node>();

	public AxonMethodVisitor(String className, int access, String methodName, String desc, String signature, String[] exceptions) {
		super(Opcodes.ASM5);
		this.className = className;
		this.access = access;
		this.methodName = methodName;
		this.desc = desc;
		this.signature = signature;
		this.exceptions = exceptions;
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		// Check out if in this method a event or command is instantiated.
		if (Opcodes.NEW == opcode && Node.get(type) != null) {
			Node node = Node.get(type);
			if (node.isEventType() || node.isCommandType()) {
				commandsAndEventsInstantiated.add(Node.get(type));
			}
		}
		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if ("apply".equals(name) && "(Ljava/lang/Object;)V".equals(desc)) {
			publishInvoked = true;
		}
		if ("publishEvent".equals(name) && owner.equals("org/axonframework/eventhandling/EventTemplate")) {
			publishInvoked = true;
		}

		if ("dispatch".equals(name) && owner.equals("org/axonframework/commandhandling/CommandBus")) {
			sendInvoked = true;
		}
		if (owner.endsWith("CommandGateway") && (name.equals("send") || name.equals("sendAndWait"))) {
			sendInvoked = true;
		}

		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visibleAtRuntime) {

		isEventHandler = desc.equals("Lorg/axonframework/saga/annotation/SagaEventHandler;")
				|| desc.equals("Lorg/axonframework/eventhandling/annotation/EventHandler;")
				|| desc.equals("Lorg/axonframework/eventsourcing/annotation/EventSourcingHandler;");

		isCommandHandler = desc.equals("Lorg/axonframework/commandhandling/annotation/CommandHandler;");
		
		isSagaHandler = desc.equals("Lorg/axonframework/saga/annotation/SagaEventHandler;");

		return super.visitAnnotation(desc, visibleAtRuntime);
	}

	@Override
	public void visitEnd() {
		
		if (isEventHandler || isCommandHandler) {
			addEventOrCommandHandlerToGraph();
		}

		if (publishInvoked) {
			addEventPublicationsToGraph();
		}

		if (sendInvoked) {
			addCommandSendsToGraph();
		}

		super.visitEnd();
	}

	private void addCommandSendsToGraph() {
		commandsAndEventsInstantiated.stream().filter(node -> node.isCommandType())
				.forEach(node -> Arc.create(methodName + " sends", Node.create(className), node));
	}

	private void addEventPublicationsToGraph() {
		commandsAndEventsInstantiated.stream().filter(node -> node.isEventType())
				.forEach(node -> Arc.create(methodName + " raises", Node.create(className), node));
	}

	private void addEventOrCommandHandlerToGraph() {
		Node handler = Node.create(className);
		Type[] argTypes = Type.getArgumentTypes(desc);
		Node handled = Node.create(argTypes[0].getInternalName());

		if(isSagaHandler) {
			handler.setSaga(); 
		}
		if (isEventHandler) {
			handled.setEventType();
			handler.setEventHandler();
		}
		if (isCommandHandler) {
			handled.setCommandType();
			handler.setCommandHandler();
		}
		
		Arc.create(methodName, handled, handler);
	}
}
