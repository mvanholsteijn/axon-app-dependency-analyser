axon-app-dependency-analyzer
============================

Start the main class and pass in any archive file(s) that you want to have analyzed. It will recursively analyse any .class file and inspect for Axon command and event handlers, event publications and command sends.

Please note that dependency analysis uses heuristics to determine which event is published and command is sent. If an event or command object is instantiated in a method and the publish() or send() is invoked in the same method it will register a dependency.  If any of these actions are executed by invoking methods on other classes, we will not see the dependency.  We are just too lazy to emulate an entire JVM :-)


