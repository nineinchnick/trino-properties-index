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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
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
    private static final String ANNOTATION_DESC = "Lio/airlift/configuration/Config;";

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
                ZipInputStream jarIS = new ZipInputStream(tarIS);

                while (true) {
                    ZipEntry jarEntry = jarIS.getNextEntry();

                    if (jarEntry == null) {
                        // no more data in jar
                        break;
                    }

                    String jarEntryName = jarEntry.getName();
                    if (!jarEntryName.endsWith(".class")) {
                        continue;
                    }
                    ClassReader classReader = new ClassReader(jarIS);
                    classReader.accept(new AnnotationSearch(ANNOTATION_DESC), 0);
                }
            }
        }
    }

    static class AnnotationSearch
            extends ClassVisitor
    {
        String searchedDescriptor;

        AnnotationSearch(String desc)
        {
            super(Opcodes.ASM6);
            searchedDescriptor = desc;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            return new MethodAnnotationSearch(searchedDescriptor);
        }
    }

    static class MethodAnnotationSearch
            extends MethodVisitor
    {
        String searchedDescriptor;

        MethodAnnotationSearch(String desc)
        {
            super(Opcodes.ASM6);
            searchedDescriptor = desc;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            if (desc.equals(searchedDescriptor)) {
                return new AnnotationValuePrinter();
            }
            return super.visitAnnotation(desc, visible);
        }
    }

    static class AnnotationValuePrinter
            extends AnnotationVisitor
    {
        AnnotationValuePrinter()
        {
            super(Opcodes.ASM6);
        }

        @Override
        public void visit(final String name, final Object value)
        {
            System.out.printf("%s%n", value);
        }
    }
}
