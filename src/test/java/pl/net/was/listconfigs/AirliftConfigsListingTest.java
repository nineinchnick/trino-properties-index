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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarInputStream;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.net.was.listconfigs.AirliftConfigsListing.readJar;

class AirliftConfigsListingTest
{
    @Test
    public void testAnnotationSearch()
    {
        String resourceName = "trino-atop-400.jar";

        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = requireNonNull(classLoader.getResource(resourceName));
        File file = new File(resource.getFile());

        JarInputStream jarIS;
        try {
            jarIS = new JarInputStream(Files.newInputStream(Paths.get(file.getAbsolutePath())));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try {
            readJar(jarIS, new PrintStream(outContent));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        String expected = """
atop.security
atop.executable-path
atop.time-zone
atop.executable-read-timeout
atop.concurrent-readers-per-node
atop.max-history-days
""";
        assertEquals(expected, outContent.toString());
    }
}
