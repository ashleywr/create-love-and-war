package net.pttheta.loveandwar.gun.content;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class ClasspathDir {
   private ClasspathDir() {
   }

   static List<String> listJsonNames(String classpathDir) {
      URL url = ClasspathDir.class.getResource(classpathDir);
      if (url == null) {
         ContentCodecs.LOGGER.error("Content directory {} is not on the classpath at all", classpathDir);
         return List.of();
      }

      try {
         List<String> names = list(url.toURI());
         if (names.isEmpty()) {
            ContentCodecs.LOGGER
               .error(
                  "Content directory {} resolved (protocol '{}') but listed no .json files - guns will not work. This is the failure mode to check first if items go inert.",
                  classpathDir,
                  url.getProtocol()
               );
         }

         return names;
      } catch (Exception e) {
         ContentCodecs.LOGGER.error("Could not list content directory {} (protocol '{}')", new Object[]{classpathDir, url.getProtocol(), e});
         return List.of();
      }
   }

   private static List<String> list(URI uri) throws IOException {
      Path dir;
      try {
         dir = Path.of(uri);
      } catch (FileSystemNotFoundException notMounted) {
         FileSystems.newFileSystem(uri, Map.of());
         dir = Path.of(uri);
      }

      if (!Files.isDirectory(dir)) {
         return List.of();
      }

      try (Stream<Path> entries = Files.list(dir)) {
         return entries.<String>map(p -> p.getFileName().toString())
            .filter(n -> n.endsWith(".json"))
            .map(n -> n.substring(0, n.length() - ".json".length()))
            .sorted()
            .toList();
      }
   }
}
