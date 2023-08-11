/*
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
package pl.net.was.listconfigs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Prints a list of annotation values.
 * It only prints values of annotations matching a specific annotation descriptor.
 * <p>
 * input: a filepath of a tar.gz file containing jar files
 * output: a list of annotation values (without annotation names)
 */
public class AirliftConfigsListing
{
    public static final String ANNOTATION_CONFIG = "Lio/airlift/configuration/Config;";
    public static final String ANNOTATION_CONFIG_DESC = "Lio/airlift/configuration/ConfigDescription;";
    public static final String ANNOTATION_LEGACY_CONFIG = "Lio/airlift/configuration/LegacyConfig;";
    public static final String ANNOTATION_DEFUNCT_CONFIG = "Lio/airlift/configuration/DefunctConfig;";
    public static final String ANNOTATION_DEPRECATED = "Lio/airlift/configuration/Deprecated;";

    public static final Set<String> ANNOTATIONS = Set.of(
            ANNOTATION_CONFIG,
            ANNOTATION_CONFIG_DESC,
            ANNOTATION_LEGACY_CONFIG,
            ANNOTATION_DEFUNCT_CONFIG,
            ANNOTATION_DEPRECATED);

    private AirliftConfigsListing()
    {
    }

    public static void main(String[] args)
            throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: airlift-list-configs <path-to-server-tar-gz>");
            System.exit(1);
        }

        Pattern pattern = Pattern.compile("/plugin/([^/]+)/");
        System.out.println("version,jar,plugin,config,description,is_deprecated");
        try (FileInputStream fileIS = new FileInputStream(args[0]);
                GZIPInputStream tarGzIS = new GZIPInputStream(fileIS);
                TarArchiveInputStream tarIS = new TarArchiveInputStream(tarGzIS)) {
            while (true) {
                TarArchiveEntry tarEntry = tarIS.getNextTarEntry();

                if (tarEntry == null) {
                    // no more data in tar archive
                    return;
                }

                String tarEntryName = tarEntry.getName();
                if (!tarEntryName.endsWith(".jar")) {
                    continue;
                }
                Jar jar = readAnnotations(tarEntryName, new ZipInputStream(tarIS));
                if (!jar.groupId.equals("io.trino")) {
                    continue;
                }
                jar.methods.forEach(method -> {
                    Map<String, Map<String, String>> annotations = method.annotationsMap();
                    Matcher matcher = pattern.matcher(jar.name);
                    String plugin = "";
                    if (matcher.find()) {
                        plugin = matcher.group(1);
                    }
                    System.out.printf("%s,%s,%s,%s,\"%s\",%s%n",
                            jar.version,
                            jar.name,
                            plugin,
                            annotations.get(ANNOTATION_CONFIG).get("value"),
                            annotations.getOrDefault(ANNOTATION_CONFIG_DESC, Map.of("value", "")).get("value").replace("\"", "\"\""),
                            annotations.containsKey(ANNOTATION_DEPRECATED));
                });
            }
        }
    }

    public static Jar readAnnotations(String filename, ZipInputStream inputStream)
            throws IOException
    {
        Set<Method> methods = new HashSet<>();
        Properties properties = new Properties();
        while (true) {
            ZipEntry jarEntry = inputStream.getNextEntry();

            if (jarEntry == null) {
                // no more data in jar
                break;
            }

            String jarEntryName = jarEntry.getName();
            if (jarEntryName.endsWith("/pom.properties")) {
                properties.load(inputStream);
                continue;
            }
            if (!jarEntryName.endsWith(".class")) {
                continue;
            }
            ClassReader classReader = new ClassReader(inputStream);
            classReader.accept(new AnnotationSearch(methods), 0);
        }
        return new Jar(
                filename,
                (String) properties.getOrDefault("groupId", ""),
                (String) properties.getOrDefault("artifactId", ""),
                (String) properties.getOrDefault("version", ""),
                methods.stream()
                        .filter(method -> method.annotations.stream().anyMatch(annotation -> annotation.name.equals(ANNOTATION_CONFIG)))
                        .collect(Collectors.toUnmodifiableSet()));
    }

    static class AnnotationSearch
            extends ClassVisitor
    {
        Set<Method> methods;

        AnnotationSearch(Set<Method> methods)
        {
            super(Opcodes.ASM9);
            this.methods = methods;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            if (ANNOTATIONS.contains(desc)) {
                Set<Annotation> annotations = new HashSet<>();
                methods.add(new Method("", annotations));
                Map<String, String> properties = new HashMap<>();
                annotations.add(new Annotation(desc, properties));
                return new AnnotationValuePrinter(properties);
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            Set<Annotation> annotations = new HashSet<>();
            methods.add(new Method(name, annotations));
            return new MethodAnnotationSearch(annotations);
        }
    }

    static class MethodAnnotationSearch
            extends MethodVisitor
    {
        Set<Annotation> annotations;

        MethodAnnotationSearch(Set<Annotation> annotations)
        {
            super(Opcodes.ASM9);
            this.annotations = annotations;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            if (ANNOTATIONS.contains(desc)) {
                Map<String, String> properties = new HashMap<>();
                annotations.add(new Annotation(desc, properties));
                return new AnnotationValuePrinter(properties);
            }
            return super.visitAnnotation(desc, visible);
        }
    }

    static class AnnotationValuePrinter
            extends AnnotationVisitor
    {
        Map<String, String> properties;

        AnnotationValuePrinter(Map<String, String> properties)
        {
            super(Opcodes.ASM9);
            this.properties = properties;
        }

        @Override
        public void visit(final String name, final Object value)
        {
            properties.put(name, value.toString());
        }
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Scope is limited")
    public record Annotation(String name, Map<String, String> properties)
    {
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Scope is limited")
    public record Method(String name, Set<Annotation> annotations)
    {
        public Map<String, Map<String, String>> annotationsMap()
        {
            return annotations.stream()
                    .collect(Collectors.toMap(Annotation::name, Annotation::properties));
        }
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Scope is limited")
    public record Jar(String name, String groupId, String artifactId, String version, Set<Method> methods)
    {
    }
}
