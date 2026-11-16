package com.itmecasey.thoroughhomesteading.script;

import com.itmecasey.thoroughhomesteading.BarelyQuestingBridge;
import com.itmecasey.thoroughhomesteading.KubeJsBridge;
import java.nio.file.Path;

/** The scripting entry points: the one API object both ModJS and KubeJS scripts use, and the installers for each optional mod. */
public final class Bridges {
    public static final HomesteadingApi API = new HomesteadingApi();

    private Bridges() {
    }

    /** Called once from each loader's mod constructor. Returns a short text of what was hooked up, for the log. */
    public static String install(Path gameDir) {
        boolean modjs = ScriptBridge.install();
        boolean kubejs = KubeJsBridge.install(gameDir);
        boolean quests = BarelyQuestingBridge.install(ScriptHost::ledger);
        return (modjs ? "ModJS " : "") + (kubejs ? "KubeJS " : "") + (quests ? "BarelyQuesting" : "");
    }
}
