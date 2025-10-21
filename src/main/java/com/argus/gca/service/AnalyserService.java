package com.argus.gca.service;

import org.objectweb.asm.*;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@Service
public class AnalyserService {

    // Suspicious methods
    private static final Set<String> SUSPICIOUS_METHODS = Set.of(
            "readObject", "readResolve", "readExternal",
            "writeObject", "writeReplace", "finalize",
            "clone", "compareTo", "<init>");

    // Risky packages
    private static final List<String> RISKY_PACKAGES = List.of(
            "org/apache/commons/collections",
            "org/apache/commons/beanutils",
            "org/springframework",
            "org/codehaus/groovy",
            "com/mchange",
            "org/python",
            "org/apache/commons/logging",
            "com/sun/rowset",
            "com/thoughtworks/xstream",
            "javassist");

    // Known sinks
    private static final Set<String> SINKS = Set.of(
            "java/lang/Runtime.exec",
            "java/lang/ProcessBuilder.start",
            "java/lang/reflect/Method.invoke",
            "java/lang/reflect/Constructor.newInstance",
            "com/sun/org/apache/xalan/internal/xsltc/trax/TemplatesImpl.newTransformer");

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    /**
     * Analyze JAR bytes in-memory.
     */
    public List<Map<String, Object>> analyzeJarBytes(byte[] jarBytes) throws IOException {
        List<Map<String, Object>> findings = new CopyOnWriteArrayList<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(jarBytes);
                JarInputStream jis = new JarInputStream(bais)) {

            List<Callable<Void>> tasks = new ArrayList<>();
            JarEntry entry;

            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.getName().endsWith(".class"))
                    continue;

                byte[] classBytes = jis.readAllBytes();
                String entryName = entry.getName();

                tasks.add(() -> {
                    analyzeClass(classBytes, entryName, findings);
                    return null;
                });
            }

            executor.invokeAll(tasks);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Analysis interrupted", e);
        }

        return findings;
    }

    private void analyzeClass(byte[] classBytes, String entryName, List<Map<String, Object>> findings) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassScanner scanner = new ClassScanner();
            reader.accept(scanner, ClassReader.SKIP_FRAMES);

            String className = scanner.getClassName();

            // Method-level findings
            if (!scanner.getSuspiciousMethods().isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                item.put("className", className);
                item.put("suspiciousMethods", scanner.getSuspiciousMethods());
                item.put("severity", computeSeverity(scanner));
                item.put("type", "method");
                findings.add(item);
            }

            // Package-level findings
            for (String pkg : RISKY_PACKAGES) {
                if (className.startsWith(pkg)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("className", className);
                    item.put("package", pkg);
                    item.put("severity", "MEDIUM");
                    item.put("type", "package");
                    findings.add(item);
                }
            }

            // Sink-level findings
            for (String sink : scanner.getInvokedMethods()) {
                if (SINKS.contains(sink)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("className", className);
                    item.put("sink", sink);
                    item.put("severity", "HIGH");
                    item.put("type", "sink");
                    findings.add(item);
                }
            }

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("className", entryName);
            error.put("type", "error");
            error.put("message", e.getMessage());
            error.put("severity", "LOW");
            findings.add(error);
        }
    }

    private String computeSeverity(ClassScanner scanner) {
        Set<String> methods = scanner.getSuspiciousMethods();
        if (methods.stream().anyMatch(m -> Set.of("readObject", "readResolve", "readExternal").contains(m))) {
            return "HIGH";
        } else if (methods.stream().anyMatch(m -> Set.of("writeObject", "writeReplace").contains(m))) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /** ASM visitor to scan methods and invoked methods */
    private static class ClassScanner extends ClassVisitor {
        private String className;
        private final Set<String> suspiciousMethods = new HashSet<>();
        private final Set<String> invokedMethods = new HashSet<>();

        public ClassScanner() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            if (SUSPICIOUS_METHODS.contains(name)) {
                suspiciousMethods.add(name);
            }

            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName,
                        String methodDesc, boolean itf) {
                    invokedMethods.add(owner + "." + methodName);
                }
            };
        }

        public String getClassName() {
            return className;
        }

        public Set<String> getSuspiciousMethods() {
            return suspiciousMethods;
        }

        public Set<String> getInvokedMethods() {
            return invokedMethods;
        }
    }
}
