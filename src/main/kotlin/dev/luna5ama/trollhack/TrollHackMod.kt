package dev.luna5ama.trollhack

import dev.luna5ama.trollhack.event.ForgeEventProcessor
import dev.luna5ama.trollhack.event.events.ShutdownEvent
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.translation.TranslationManager
import dev.luna5ama.trollhack.util.ConfigUtils
import dev.luna5ama.trollhack.util.threads.TimerScope
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.opengl.Display
import java.awt.Dimension
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.JOptionPane
import javax.swing.UIManager

@Mod(
    modid = TrollHackMod.ID,
    name = TrollHackMod.NAME,
    version = TrollHackMod.VERSION
)
class TrollHackMod {
    @Suppress("UNUSED_PARAMETER")
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        val directory = File("${DIRECTORY}/")
        if (!directory.exists()) directory.mkdir()
        LoaderWrapper.preLoadAll()
        TranslationManager.checkUpdate()
        Thread.currentThread().priority = Thread.MAX_PRIORITY
        //verifyHWID()
    }

    @Suppress("UNUSED_PARAMETER")
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        LoaderWrapper.loadAll()
        MinecraftForge.EVENT_BUS.register(ForgeEventProcessor)
        ConfigUtils.loadAll()
        TimerScope.start()
        MainFontRenderer.reloadFonts()
    }

    @Suppress("UNUSED_PARAMETER")
    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        ready = true
        Runtime.getRuntime().addShutdownHook(Thread {
            ShutdownEvent.post()
            ConfigUtils.saveAll()
        })
    }

/*
    private fun verifyHWID() {
        try {

            val url = ""
            val connection = URL(url).openStream()
            val reader = BufferedReader(InputStreamReader(connection))
            val whitelist = reader.readText()
            reader.close()

            if (!whitelist.contains(HWID.getHWID())) {
                JOptionPane.showMessageDialog(
                    null,
                    "Your HWID is not on the whitelist.  :(\nYour HWID: ${HWID.getHWID()}",
                    "404 HWID-auth",
                    JOptionPane.INFORMATION_MESSAGE
                )
                System.exit(0)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "An error occurred during authentication. Please check your internet connection.",
                "404 HWID-auth",
                JOptionPane.ERROR_MESSAGE
            )
            System.exit(0)
       }
    }
*/

    private fun getMinecraftID(): String {
        return net.minecraft.client.Minecraft.getMinecraft().session.username
    }

    companion object {
        const val NAME = Metadata.NAME
        const val ID = Metadata.ID
        const val VERSION = Metadata.VERSION
        const val DIRECTORY = ID

        @JvmField
        val title: String = Display.getTitle()

        @JvmField
        val logger: Logger = LogManager.getLogger(NAME)

        @JvmStatic
        @get:JvmName("isReady")
        var ready = false; private set
    }
}
