package com.example.vapeon_movil.utils

import com.example.vapeon_movil.R

/**
 * Resuelve dinámicamente la imagen drawable de producto según el contexto del producto (nombre/marca/categoría)
 * y el sabor seleccionado.
 *
 * Discrimina entre productos de tipo **Kit Completo** (`kitcompleto`) y **Recargas/Cartuchos** (`recarga`).
 */
fun resolverImagenProducto(nombreOContexto: String, saborSeleccionado: String?): Int {
    val contextoNorm = normalizarTextoImagen(nombreOContexto)
    val saborNorm = normalizarTextoImagen(saborSeleccionado.orEmpty())

    val esKitCompleto = contextoNorm.contains("kit") || contextoNorm.contains("completo") ||
            saborNorm.contains("kit") || saborNorm.contains("completo")

    return when {
        // ── LIFEPOD ─────────────────────────────────────────────────────────
        contextoNorm.contains("lifepod") -> {
            if (esKitCompleto) {
                when {
                    saborNorm.contains("polar") -> R.drawable.muestra_lifepod_polarice_kitcompleto
                    saborNorm.contains("pink") || saborNorm.contains("lemonade") -> R.drawable.muestra_lifepod_pinklemonade_kitcompleto
                    saborNorm.contains("banana") || saborNorm.contains("custard") -> R.drawable.muestra_lifepod_bananacustard_kitcompleto
                    saborNorm.contains("blackberry") -> R.drawable.muestra_lifepod_blackberryice_kitcompleto
                    saborNorm.contains("summer") || saborNorm.contains("love") -> R.drawable.muestra_lifepod_summerlove_kitcompleto
                    else -> R.drawable.muestra_lifepod_completa_kitcompleto
                }
            } else {
                when {
                    saborNorm.contains("grape") -> R.drawable.muestra_lifepod_grapeice_recarga
                    saborNorm.contains("polar") -> R.drawable.muestra_lifepod_polarice_recarga
                    saborNorm.contains("miami") || saborNorm.contains("mint") -> R.drawable.muestra_lifepod_miamimint_recarga
                    saborNorm.contains("capuchino") || saborNorm.contains("capucchino") -> R.drawable.muestra_lifepod_capucchino_recarga
                    saborNorm.contains("greenappleice") -> R.drawable.muestra_lifepod_greenappleice_recarga
                    saborNorm.contains("greenapple") || saborNorm.contains("apple") -> R.drawable.muestra_lifepod_greenapple_recarga
                    saborNorm.contains("monster") -> R.drawable.muestra_lifepod_monster_recarga
                    saborNorm.contains("dragon") || saborNorm.contains("dragonfruit") -> R.drawable.muestra_lifepod_dragonfruitice_recarga
                    saborNorm.contains("passionfruit") -> R.drawable.muestra_lifepod_passionfruitice_recarga
                    saborNorm.contains("passionmango") -> R.drawable.muestra_lifepod_passionmangoice_recarga
                    saborNorm.contains("blueberry") || saborNorm.contains("strawberry") -> R.drawable.muestra_lifepod_blueberrystrawberry_recarga
                    saborNorm.contains("watermelon") || saborNorm.contains("peach") -> R.drawable.muestra_lifepod_watermelonpeachice_recarga
                    else -> R.drawable.muestra_lifepod_completa_kitcompleto
                }
            }
        }

        // ── ARGUS ───────────────────────────────────────────────────────────
        contextoNorm.contains("argus") -> when {
            saborNorm.contains("mrblue") || saborNorm.contains("blue") -> R.drawable.muestra_argus_mrblue_kitcompleto
            saborNorm.contains("triplemango") || saborNorm.contains("mango") -> R.drawable.muestra_argus_triplemango_kitcompleto
            else -> R.drawable.muestra_argus_completa_kitcompleto
        }

        // ── NEXA ────────────────────────────────────────────────────────────
        contextoNorm.contains("nexa") -> when {
            saborNorm.contains("bluerazz") || saborNorm.contains("blue") -> R.drawable.muestra_nexa_bluerazzice_kitcompleto
            saborNorm.contains("sourapple") || saborNorm.contains("sour") -> R.drawable.muestra_nexa_sourappleice_kitcompleto
            saborNorm.contains("strawberry") -> R.drawable.muestra_nexa_strawberryice_kitcompleto
            else -> R.drawable.muestra_nexa_bluerazzice_kitcompleto
        }

        // ── CRABE ───────────────────────────────────────────────────────────
        contextoNorm.contains("crabe") -> when {
            saborNorm.contains("mango") -> R.drawable.muestra_crabe_mangoice_descartable
            saborNorm.contains("strawberry") -> R.drawable.muestra_crabe_strawberryice_descartable
            else -> R.drawable.muestra_crabe_mangoice_descartable
        }

        // ── EASE ────────────────────────────────────────────────────────────
        contextoNorm.contains("ease") -> when {
            saborNorm.contains("jungle") || saborNorm.contains("breeze") -> R.drawable.muestra_ease_junglebreeze_descartable
            saborNorm.contains("lush") || saborNorm.contains("clear") -> R.drawable.muestra_ease_lushice_y_clear5_descartables
            saborNorm.contains("pine") || saborNorm.contains("pineapple") -> R.drawable.muestra_ease_pineapls_descartable
            else -> R.drawable.muestra_ease_lushice_y_clear5_descartables
        }

        else -> R.drawable.logo_vapeon
    }
}

private fun normalizarTextoImagen(texto: String): String {
    return texto.lowercase()
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")
        .replace("|", "")
        .replace("/", "")
        .replace("+", "")
        .replace("%", "")
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
}
