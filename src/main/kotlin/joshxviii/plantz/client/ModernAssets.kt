package joshxviii.plantz.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import joshxviii.plantz.PazCompatSounds
import joshxviii.plantz.PazMain
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.AbstractPackResources
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.IoSupplier
import net.neoforged.neoforge.event.AddPackFindersEvent
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.zip.ZipFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Some 26.1 content is game assets rather than code: the baby armor textures and the sounds in [PazCompatSounds]. The
 * mod doesn't ship Mojang's files; when a 26.1 or newer game is installed on the machine (vanilla launcher or Prism
 * Launcher), this built-in client pack serves those files from it. Without one, the 1.21.1 substitutes stay.
 */
object ModernAssets {
    private const val PACK_ID = "plantz:modern_assets"
    private const val BABY_ARMOR_DIR = "assets/minecraft/textures/entity/equipment/humanoid_baby/"

    private val files: Map<String, ByteArray> by lazy { collect() }

    fun addPackFinders(event: AddPackFindersEvent) {
        if (event.packType != PackType.CLIENT_RESOURCES) return
        event.addRepositorySource { consumer ->
            if (files.isEmpty()) return@addRepositorySource
            val info = PackLocationInfo(PACK_ID, Component.literal("Plants & Zombies: 26.1 assets from your game files"), PackSource.BUILT_IN, Optional.empty())
            val supplier = object : Pack.ResourcesSupplier {
                override fun openPrimary(location: PackLocationInfo): PackResources = MemoryPack(location, files)
                override fun openFull(location: PackLocationInfo, metadata: Pack.Metadata): PackResources = MemoryPack(location, files)
            }
            Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES, PackSelectionConfig(true, Pack.Position.TOP, false))?.let { consumer.accept(it) }
        }
    }

    /** A 26.1+ game install: the client jar, and its asset index with the objects directory (sounds). */
    private data class Install(val version: String, val clientJar: Path, val assetIndex: Path?, val objects: Path?)

    private fun collect(): Map<String, ByteArray> {
        val install = try {
            findInstalls().firstOrNull()
        } catch (e: Exception) {
            PazMain.LOGGER.warn("Couldn't look for a 26.1 game install", e)
            null
        }
        if (install == null) {
            PazMain.LOGGER.info("No 26.1+ game files found; using the 1.21.1 substitutes for 26.1 baby armor textures and sounds")
            return emptyMap()
        }
        val out = linkedMapOf<String, ByteArray>()
        try {
            ZipFile(install.clientJar.toFile()).use { jar ->
                jar.entries().asSequence().filter { !it.isDirectory && it.name.startsWith(BABY_ARMOR_DIR) && it.name.endsWith(".png") }
                    .forEach { out[it.name] = jar.getInputStream(it).use(InputStream::readBytes) }
            }
        } catch (e: Exception) {
            PazMain.LOGGER.warn("Couldn't read {}", install.clientJar, e)
        }
        val sounds = if (install.assetIndex != null && install.objects != null) collectSounds(install.assetIndex, install.objects, out) else 0
        if (out.isNotEmpty()) out["pack.mcmeta"] = """{"pack":{"pack_format":34,"description":"26.1 assets from ${install.version}"}}""".toByteArray()
        PazMain.LOGGER.info("Using 26.1 assets from Minecraft {} ({} baby armor textures, {} of {} sounds)",
            install.version, out.keys.count { it.startsWith(BABY_ARMOR_DIR) }, sounds, PazCompatSounds.entries.size)
        return out
    }

    /** Copies the sound files of each [PazCompatSounds] event and writes sounds.json entries that replace the substitutes. */
    private fun collectSounds(indexFile: Path, objects: Path, out: MutableMap<String, ByteArray>): Int {
        val index = JsonParser.parseString(indexFile.readText()).asJsonObject.getAsJsonObject("objects")
        fun objectBytes(key: String): ByteArray? {
            val hash = index.getAsJsonObject(key)?.get("hash")?.asString ?: return null
            val file = objects.resolve(hash.substring(0, 2)).resolve(hash)
            return if (file.exists()) Files.readAllBytes(file) else null
        }
        val vanillaSounds = objectBytes("minecraft/sounds.json")?.let { JsonParser.parseString(String(it)).asJsonObject } ?: return 0
        val definitions = JsonObject()
        for (sound in PazCompatSounds.entries) {
            val definition = vanillaSounds.getAsJsonObject(sound.id) ?: continue
            val entries = JsonArray()
            val sounds = mutableMapOf<String, ByteArray>()
            var complete = true
            for (element in definition.getAsJsonArray("sounds")) {
                val entry = if (element.isJsonPrimitive) JsonObject().apply { addProperty("name", element.asString) } else element.asJsonObject.deepCopy()
                if (entry.get("type")?.asString == "event") { complete = false; break }
                val name = entry.get("name").asString
                val bytes = objectBytes("minecraft/sounds/$name.ogg")
                if (bytes == null) { complete = false; break }
                sounds["assets/minecraft/sounds/$name.ogg"] = bytes
                entry.addProperty("name", "minecraft:$name")
                entries.add(entry)
            }
            if (!complete) continue
            out.putAll(sounds)
            definitions.add("compat." + sound.id, JsonObject().apply {
                addProperty("replace", true)
                add("sounds", entries)
                definition.get("subtitle")?.let { add("subtitle", it) }
            })
        }
        if (definitions.size() > 0) out["assets/plantz/sounds.json"] = definitions.toString().toByteArray()
        return definitions.size()
    }

    /** Vanilla launcher and Prism Launcher installs of 26.1 or newer, 26.1.x first (the version upstream targets). */
    private fun findInstalls(): List<Install> {
        val home = Path.of(System.getProperty("user.home"))
        val os = System.getProperty("os.name").lowercase()
        val appData = System.getenv("APPDATA")?.let(Path::of)
        val minecraftDirs = buildList {
            appData?.let { add(it.resolve(".minecraft")) }
            add(home.resolve(".minecraft"))
            if ("mac" in os) add(home.resolve("Library/Application Support/minecraft"))
        }
        val prismDirs = buildList {
            appData?.let { add(it.resolve("PrismLauncher")) }
            add(home.resolve(".local/share/PrismLauncher"))
            if ("mac" in os) add(home.resolve("Library/Application Support/PrismLauncher"))
        }
        val installs = mutableListOf<Install>()
        for (dir in minecraftDirs.filter { it.resolve("versions").isDirectory() }) {
            for (versionDir in dir.resolve("versions").listDirectoryEntries().filter { it.isDirectory() && isModern(it.name) }) {
                val jar = versionDir.resolve(versionDir.name + ".jar")
                val json = versionDir.resolve(versionDir.name + ".json")
                if (!jar.exists()) continue
                val indexId = if (json.exists()) assetIndexId(json) else null
                installs += Install(versionDir.name, jar, indexId?.let { dir.resolve("assets/indexes/$it.json") }?.takeIf { it.exists() }, dir.resolve("assets/objects"))
            }
        }
        for (dir in prismDirs.filter { it.resolve("libraries/com/mojang/minecraft").isDirectory() }) {
            for (versionDir in dir.resolve("libraries/com/mojang/minecraft").listDirectoryEntries().filter { it.isDirectory() && isModern(it.name) }) {
                val jar = versionDir.resolve("minecraft-${versionDir.name}-client.jar")
                if (!jar.exists()) continue
                val meta = dir.resolve("meta/net.minecraft/${versionDir.name}.json")
                val indexId = if (meta.exists()) assetIndexId(meta) else null
                installs += Install(versionDir.name, jar, indexId?.let { dir.resolve("assets/indexes/$it.json") }?.takeIf { it.exists() }, dir.resolve("assets/objects"))
            }
        }
        return installs.sortedWith(compareBy<Install>({ !it.version.startsWith("26.1") }, { it.assetIndex == null }, { versionKey(it.version) }))
    }

    private fun assetIndexId(versionJson: Path): String? = try {
        JsonParser.parseString(versionJson.readText()).asJsonObject.getAsJsonObject("assetIndex")?.get("id")?.asString
    } catch (e: Exception) {
        null
    }

    private val VERSION = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?(?:-.+)?$""")

    /** 26.1 and later (year-based versions). */
    private fun isModern(version: String): Boolean {
        val m = VERSION.matchEntire(version) ?: return false
        val major = m.groupValues[1].toInt()
        val minor = m.groupValues[2].toInt()
        return major > 26 || (major == 26 && minor >= 1)
    }

    private fun versionKey(version: String): Int {
        val m = VERSION.matchEntire(version) ?: return Int.MAX_VALUE
        return m.groupValues[1].toInt() * 10000 + m.groupValues[2].toInt() * 100 + (m.groupValues[3].toIntOrNull() ?: 0)
    }

    /** A resource pack held in memory. */
    private class MemoryPack(location: PackLocationInfo, private val files: Map<String, ByteArray>) : AbstractPackResources(location) {
        override fun getRootResource(vararg elements: String): IoSupplier<InputStream>? = supplier(elements.joinToString("/"))

        override fun getResource(packType: PackType, location: ResourceLocation): IoSupplier<InputStream>? =
            supplier("${packType.directory}/${location.namespace}/${location.path}")

        override fun listResources(packType: PackType, namespace: String, path: String, output: PackResources.ResourceOutput) {
            val prefix = "${packType.directory}/$namespace/"
            for (key in files.keys) {
                if (!key.startsWith("$prefix$path/")) continue
                ResourceLocation.tryBuild(namespace, key.removePrefix(prefix))?.let { output.accept(it, supplier(key)!!) }
            }
        }

        override fun getNamespaces(type: PackType): Set<String> =
            files.keys.filter { it.startsWith(type.directory + "/") }.map { it.split('/')[1] }.toSet()

        override fun close() {}

        private fun supplier(key: String): IoSupplier<InputStream>? = files[key]?.let { bytes -> IoSupplier { bytes.inputStream() } }
    }
}
