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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.net.was.listconfigs.AirliftConfigsListing.ANNOTATION_CONFIG;
import static pl.net.was.listconfigs.AirliftConfigsListing.ANNOTATION_CONFIG_DESC;
import static pl.net.was.listconfigs.AirliftConfigsListing.readAnnotations;

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
        AirliftConfigsListing.Jar result;
        try {
            result = readAnnotations("foo", jarIS);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, Set<AirliftConfigsListing.Annotation>> indexed = result.methods().stream()
                .collect(Collectors.toMap(AirliftConfigsListing.Method::name, AirliftConfigsListing.Method::annotations));
        Map<String, Set<AirliftConfigsListing.Annotation>> expected = Map.of(
                "setSecurity", Set.of(
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG, Map.of("value", "atop.security"))),
                "setExecutablePath", Set.of(
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG, Map.of("value", "atop.executable-path"))),
                "setTimeZone", Set.of(
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG, Map.of("value", "atop.time-zone")),
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG_DESC, Map.of("value", "The timezone in which the atop data was collected. Generally the timezone of the host."))),
                "setReadTimeout", Set.of(
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG, Map.of("value", "atop.executable-read-timeout")),
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG_DESC, Map.of("value", "The timeout when reading from the atop process."))),
                "setConcurrentReadersPerNode", Set.of(
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG, Map.of("value", "atop.concurrent-readers-per-node"))),
                "setMaxHistoryDays", Set.of(
                        new AirliftConfigsListing.Annotation(ANNOTATION_CONFIG, Map.of("value", "atop.max-history-days"))));
        assertThat(indexed.keySet()).isEqualTo(expected.keySet());
        indexed.forEach((key, value) -> assertThat(value).containsExactlyInAnyOrderElementsOf(expected.get(key)));
    }
}
