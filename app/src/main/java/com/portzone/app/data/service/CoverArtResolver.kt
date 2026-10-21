package com.portzone.app.data.service

import java.util.Locale

/**
 * Provedor Inteligente de Capas e Box Arts para Jogos e Ports.
 *
 * Fornece URLs de alta resolução diretamente dos repositórios oficiais de Box Art
 * (Libretro Thumbnails e Steam Store CDN) para mais de 100 jogos e franquias consagradas.
 */
object CoverArtResolver {

    private const val STEAM_CDN = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps"
    private const val LIBRETRO_N64 = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Nintendo_64/master/Named_Boxarts"
    private const val LIBRETRO_GC = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_GameCube/master/Named_Boxarts"
    private const val LIBRETRO_NES = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Nintendo_Entertainment_System/master/Named_Boxarts"
    private const val LIBRETRO_SNES = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Super_Nintendo_Entertainment_System/master/Named_Boxarts"
    private const val LIBRETRO_GBC = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy_Color/master/Named_Boxarts"
    private const val LIBRETRO_GBA = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy_Advance/master/Named_Boxarts"
    private const val LIBRETRO_PS1 = "https://raw.githubusercontent.com/libretro-thumbnails/Sony_-_PlayStation/master/Named_Boxarts"
    private const val LIBRETRO_PS2 = "https://raw.githubusercontent.com/libretro-thumbnails/Sony_-_PlayStation_2/master/Named_Boxarts"
    private const val LIBRETRO_DC = "https://raw.githubusercontent.com/libretro-thumbnails/Sega_-_Dreamcast/master/Named_Boxarts"

    // Mapeamento direto de franquias e jogos para o ID oficial da Steam
    private val STEAM_APP_IDS = mapOf(
        "undertale" to "391540",
        "deltarune" to "1671210",
        "resident evil 4" to "2050650",
        "resident evil 2" to "883710",
        "resident evil 3" to "952060",
        "resident evil" to "304240",
        "ultra street fighter iv" to "45760",
        "street fighter iv" to "45760",
        "transport tycoon deluxe" to "1536610",
        "openttd" to "1536610",
        "team fortress 2" to "440",
        "team fortress" to "440",
        "the elder scrolls 3" to "22320",
        "morrowind" to "22320",
        "daggerfall" to "1812390",
        "star wars jedi knight ii" to "6030",
        "jedi outcast" to "6030",
        "star wars jedi knight" to "6020",
        "jedi academy" to "6020",
        "star wars dark forces" to "2820",
        "dark forces" to "2820",
        "strife" to "317040",
        "sonic cd" to "200940",
        "sonic 2" to "71113",
        "sonic the hedgehog" to "71113",
        "sonic mania" to "584400",
        "s.t.a.l.k.e.r." to "4500",
        "stalker" to "4500",
        "shadow of chernobyl" to "4500",
        "clear sky" to "20510",
        "call of pripyat" to "41700",
        "sleeping dogs" to "307690",
        "shadow warrior" to "238070",
        "rise of the triad" to "214900",
        "return to castle wolfenstein" to "9010",
        "wolfenstein 3d" to "2270",
        "spear of destiny" to "2270",
        "quake" to "2310",
        "quake ii" to "2320",
        "quake 2" to "2320",
        "quake iii" to "2200",
        "quake 3" to "2200",
        "quake 4" to "930",
        "redneck rampage" to "565550",
        "rayman origins" to "207490",
        "half-life 2" to "220",
        "half-life" to "70",
        "portal 2" to "620",
        "portal" to "400",
        "left 4 dead 2" to "550",
        "left 4 dead" to "500",
        "doom ii" to "2300",
        "doom 2" to "2300",
        "final doom" to "2290",
        "doom 3" to "208200",
        "doom 64" to "1148590",
        "doom" to "2280",
        "duke nukem 3d" to "225140",
        "duke nukem" to "225140",
        "blood" to "1010750",
        "heretic" to "2390",
        "hexen" to "2360",
        "hexen ii" to "2370",
        "hollow knight" to "367520",
        "cuphead" to "268910",
        "celeste" to "504230",
        "dead cells" to "588650",
        "shovelry" to "250760",
        "shovel knight" to "250760",
        "stardew valley" to "413150",
        "fallout 2" to "38410",
        "fallout" to "38400",
        "grand theft auto iii" to "12100",
        "gta iii" to "12100",
        "vice city" to "12110",
        "san andreas" to "12120",
        "max payne 2" to "12150",
        "max payne" to "12140",
        "xcom" to "7760",
        "openxcom" to "7760",
        "openra" to "2820",
        "rollercoaster tycoon" to "285330",
        "openrct2" to "285330",
        "heroes of might and magic iii" to "338840",
        "heroes of might and magic" to "338840",
        "deus ex" to "6910",
        "system shock 2" to "238210",
        "system shock" to "410710",
        "thief" to "211600",
        "tomb raider" to "224960",
        "alien vs predator" to "10680",
        "silent hill" to "2124490"
    )

    /**
     * Resolve uma URL de capa oficial com base no nome do jogo ou pasta.
     */
    fun obterUrlCapa(nomeJogoOuPasta: String): String? {
        val limpo = normalizarNome(nomeJogoOuPasta)
        val lower = limpo.lowercase(Locale.ROOT)

        // 1. Tenta mapeamento na Steam CDN
        for ((chave, appId) in STEAM_APP_IDS) {
            if (lower.contains(chave)) {
                return "$STEAM_CDN/$appId/header.jpg"
            }
        }

        // 2. Tenta mapeamento em consoles clássicos via Libretro Boxarts
        return when {
            // Nintendo 64
            lower.contains("super mario 64") -> "$LIBRETRO_N64/Super%20Mario%2064%20(USA).png"
            lower.contains("zelda") && lower.contains("ocarina") -> "$LIBRETRO_N64/Legend%20of%20Zelda,%20The%20-%20Ocarina%20of%20Time%20(USA).png"
            lower.contains("zelda") && lower.contains("majora") -> "$LIBRETRO_N64/Legend%20of%20Zelda,%20The%20-%20Majora's%20Mask%20(USA).png"
            lower.contains("star fox") -> "$LIBRETRO_N64/Star%20Fox%2064%20(USA).png"
            lower.contains("banjo") && lower.contains("kazooie") -> "$LIBRETRO_N64/Banjo-Kazooie%20(USA).png"
            lower.contains("diddy kong") -> "$LIBRETRO_N64/Diddy%20Kong%20Racing%20(USA).png"
            lower.contains("goldeneye") -> "$LIBRETRO_N64/GoldenEye%20007%20(USA).png"
            lower.contains("mario kart 64") -> "$LIBRETRO_N64/Mario%20Kart%2064%20(USA).png"

            // GameCube
            lower.contains("super mario sunshine") || lower.contains("sunpad") -> "$LIBRETRO_GC/Super%20Mario%20Sunshine%20(USA).png"
            lower.contains("metroid prime") -> "$LIBRETRO_GC/Metroid%20Prime%20(USA).png"
            lower.contains("wind waker") -> "$LIBRETRO_GC/Legend%20of%20Zelda,%20The%20-%20The%20Wind%20Waker%20(USA).png"

            // SNES & NES
            lower.contains("super mario world") -> "$LIBRETRO_SNES/Super%20Mario%20World%20(USA).png"
            lower.contains("super metroid") -> "$LIBRETRO_SNES/Super%20Metroid%20(Japan,%20USA).png"
            lower.contains("link to the past") || lower.contains("zelda3") -> "$LIBRETRO_SNES/Legend%20of%20Zelda,%20The%20-%20A%20Link%20to%20the%20Past%20(USA).png"
            lower.contains("super mario bros") -> "$LIBRETRO_NES/Super%20Mario%20Bros.%20(World).png"

            // Game Boy & GBA
            lower.contains("link's awakening") || lower.contains("ladx") -> "$LIBRETRO_GBC/Legend%20of%20Zelda,%20The%20-%20Link's%20Awakening%20DX%20(USA,%20Europe).png"
            lower.contains("minish cap") -> "$LIBRETRO_GBA/Legend%20of%20Zelda,%20The%20-%20The%20Minish%20Cap%20(USA).png"
            lower.contains("metroid fusion") -> "$LIBRETRO_GBA/Metroid%20Fusion%20(USA).png"
            lower.contains("pokemon fire") || lower.contains("firered") -> "$LIBRETRO_GBA/Pokemon%20-%20FireRed%20Version%20(USA).png"
            lower.contains("pokemon emerald") -> "$LIBRETRO_GBA/Pokemon%20-%20Emerald%20Version%20(USA).png"

            // PlayStation 1 & 2
            lower.contains("tekken 3") -> "$LIBRETRO_PS1/Tekken%203%20(USA).png"
            lower.contains("tekken 2") -> "$LIBRETRO_PS1/Tekken%202%20(USA).png"
            lower.contains("symphony of the night") || lower.contains("sotn") -> "$LIBRETRO_PS1/Castlevania%20-%20Symphony%20of%20the%20Night%20(USA).png"
            lower.contains("crash bandicoot") -> "$LIBRETRO_PS1/Crash%20Bandicoot%20(USA).png"
            lower.contains("spyro") -> "$LIBRETRO_PS1/Spyro%20the%20Dragon%20(USA).png"
            lower.contains("metal gear solid") -> "$LIBRETRO_PS1/Metal%20Gear%20Solid%20(USA).png"
            lower.contains("tony hawk") -> "$LIBRETRO_PS1/Tony%20Hawk's%20Pro%20Skater%202%20(USA).png"

            // Dreamcast
            lower.contains("shenmue") -> "$LIBRETRO_DC/Shenmue%20(USA).png"
            lower.contains("jet set radio") || lower.contains("jet grind radio") -> "$LIBRETRO_DC/Jet%20Grind%20Radio%20(USA).png"
            lower.contains("crazy taxi") -> "$LIBRETRO_DC/Crazy%20Taxi%20(USA).png"

            else -> null
        }
    }

    /**
     * Limpa prefixos, extensões e sufixos técnicos para extrair o título puro do jogo.
     */
    fun normalizarNome(nomeBruto: String): String {
        return nomeBruto
            .replace(Regex("(?i)\\.apk|\\.zip|\\.7z|\\.z64|\\.url|\\.txt"), "")
            .replace(Regex("\\s*\\(\\d+\\)$"), "")
            .replace(Regex("(?i)\\s*\\(link\\s*/\\s*instruções\\)"), "")
            .replace("_", " ")
            .split(" - ")[0]
            .trim()
    }

    /**
     * Extrai iniciais estilizadas para o badge gamer caso a capa não esteja disponível.
     * Exemplo: "Resident Evil 4" -> "RE4", "Undertale" -> "UT", "Quake II" -> "Q2".
     */
    fun extrairIniciais(nomeJogo: String): String {
        val limpo = normalizarNome(nomeJogo)
        val palavras = limpo.split(" ").filter { it.isNotBlank() }

        if (palavras.isEmpty()) return "PZ"

        // Se tiver números no final (ex: "Resident Evil 4", "Tekken 3", "Quake 2")
        val numeroFinal = Regex("\\d+").find(limpo)?.value

        if (palavras.size == 1) {
            val unica = palavras[0]
            return if (unica.length >= 2) unica.take(2).uppercase(Locale.ROOT) else unica.uppercase(Locale.ROOT)
        }

        if (numeroFinal != null) {
            val primeiraLetra = palavras[0].take(1).uppercase(Locale.ROOT)
            val segundaLetra = palavras.getOrNull(1)?.take(1)?.uppercase(Locale.ROOT) ?: ""
            return "$primeiraLetra$segundaLetra$numeroFinal"
        }

        return palavras.take(3).map { it.take(1).uppercase(Locale.ROOT) }.joinToString("")
    }
}
