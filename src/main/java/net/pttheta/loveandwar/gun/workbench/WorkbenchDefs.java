package net.pttheta.loveandwar.gun.workbench;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkbenchDefs {
   private static final Logger LOGGER = LoggerFactory.getLogger("createloveandwar/gun-workbench");
   private static final String MOD_ID = "createloveandwar";
   private static final List<String> NAMES = List.of(
      "law_workbench", "advanced_workbench", "expert_workbench", "em2_workbench", "mas62_workbench", "stg57_workbench"
   );
   private static final Map<String, WorkbenchDef> DEFS = load();

   private WorkbenchDefs() {
   }

   public static Optional<WorkbenchDef> get(String workbenchName) {
      return Optional.ofNullable(DEFS.get(workbenchName));
   }

   private static Map<String, WorkbenchDef> load() {
      Map<String, WorkbenchDef> out = new LinkedHashMap<>();

      for (String name : NAMES) {
         String path = "/data/createloveandwar/workbenches/" + name + ".json";

         try (InputStream in = WorkbenchDefs.class.getResourceAsStream(path)) {
            if (in != null) {
               JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
               WorkbenchDef.CODEC
                  .parse(JsonOps.INSTANCE, json)
                  .resultOrPartial(msg -> LOGGER.error("Failed to parse workbench {}: {}", name, msg))
                  .ifPresent(def -> out.put(name, def));
            } else {
               LOGGER.warn("No workbench data at {}", path);
            }
         } catch (Exception e) {
            LOGGER.error("Could not read workbench data {}", path, e);
         }
      }

      return out;
   }
}
