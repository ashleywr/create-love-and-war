package net.pttheta.loveandwar.gametest;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@EventBusSubscriber(modid = "createloveandwar")
public final class GameTestReportSetup {
    @SubscribeEvent
    public static void configureReport(ServerAboutToStartEvent event) {
        String path = System.getProperty("law.gameTestReport");
        if (!(event.getServer() instanceof GameTestServer) || path == null) return;
        File destination = new File(path);
        destination.getParentFile().mkdirs();
        try {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(destination));
        } catch (ParserConfigurationException exception) {
            throw new IllegalStateException("Cannot initialize GameTest XML report", exception);
        }
    }
}
