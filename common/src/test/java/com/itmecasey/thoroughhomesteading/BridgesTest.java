package com.itmecasey.thoroughhomesteading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The optional mods are really optional: with none of them on the class path everything reports "absent" and does nothing. */
class BridgesTest {
    @Test
    void kubeJsHelperIsWrittenOnceAndNeverOverwritten(@TempDir Path dir) throws IOException {
        assertTrue(KubeJsBridge.writeHelper(dir));
        Path file = dir.resolve("kubejs/startup_scripts/thoroughhomesteading.js");
        String text = Files.readString(file);
        assertTrue(text.contains("global.Homesteading ="));
        assertTrue(text.contains("global.HomesteadingEvents ="));
        Files.writeString(file, "// mine\n");
        assertTrue(KubeJsBridge.writeHelper(dir));
        assertEquals("// mine\n", Files.readString(file));
    }

    @Test
    void nothingHappensWithoutKubeJs(@TempDir Path dir) {
        assertFalse(KubeJsBridge.present());
        assertFalse(KubeJsBridge.install(dir));
        assertFalse(Files.exists(dir.resolve("kubejs")));
    }

    @Test
    void nothingHappensWithoutBarelyQuesting() {
        assertFalse(BarelyQuestingBridge.present());
        assertFalse(BarelyQuestingBridge.install(Ledger::new));
        UUID a = UUID.randomUUID();
        assertTrue(BarelyQuestingBridge.sameTeam(a, a));
        assertFalse(BarelyQuestingBridge.sameTeam(a, UUID.randomUUID()));
        assertFalse(BarelyQuestingBridge.sameTeam(null, a));
    }

    @Test
    void theKubeJsScriptNamesRealClasses() {
        assertTrue(KubeJsBridge.SCRIPT.contains("com.itmecasey.thoroughhomesteading.script.Bridges"));
        assertTrue(KubeJsBridge.SCRIPT.contains("com.itmecasey.thoroughhomesteading.HomesteadingEvents"));
        assertEquals("HomesteadingEvents", HomesteadingEvents.class.getSimpleName());
    }
}
