package com.auris.app

data class Condition(
    val id: String,
    val name: String,
    val short: String,
    val signs: String
)

/**
 * The conditions Auris knows how to describe. The ids match the class folder names
 * the training notebook produces, so a model trained on any subset of them works.
 */
object Catalogue {

    val conditions: List<Condition> = listOf(
        Condition(
            "normal", "Normal tympanic membrane", "Normal",
            "Pearly grey, translucent drum in a neutral position; cone of light antero-inferiorly; malleus handle and lateral process visible; healthy canal skin."
        ),
        Condition(
            "wax", "Cerumen (wax) impaction", "Wax",
            "Yellow, brown or black wax filling the canal; drum partly or completely hidden."
        ),
        Condition(
            "aom", "Acute otitis media", "AOM",
            "Moderate to marked bulging; opaque, red or yellow drum; landmarks and light reflex lost; fresh discharge if it has perforated."
        ),
        Condition(
            "ome", "Otitis media with effusion", "OME",
            "Dull amber, grey or bluish drum, often retracted; air–fluid level or bubbles; no bulging or acute inflammation."
        ),
        Condition(
            "csom", "CSOM / tympanic membrane perforation", "CSOM / perforation",
            "Perforation of the pars tensa with middle-ear mucosa visible; mucopurulent discharge when active, dry when inactive."
        ),
        Condition(
            "oe", "Acute otitis externa", "Otitis externa",
            "Red, swollen canal skin with a narrowed lumen and moist debris or discharge; drum often obscured."
        ),
        Condition(
            "otomycosis", "Otomycosis (fungal)", "Otomycosis",
            "Fluffy white or cream hyphae, or black or grey spore-dotted debris (“wet newspaper”) in the canal."
        ),
        Condition(
            "tympanosclerosis", "Tympanosclerosis", "Tympanosclerosis",
            "Chalky white plaques in an otherwise intact drum, often horseshoe-shaped."
        ),
        Condition(
            "cholesteatoma", "Retraction pocket / cholesteatoma", "Cholesteatoma",
            "Attic or postero-superior retraction with white keratin debris, attic crust, bone erosion or granulation; often foul discharge."
        ),
        Condition(
            "grommet", "Ventilation tube (grommet)", "Grommet",
            "A small tube or eyelet seated in the drum; check its position, patency and any extrusion."
        ),
        Condition(
            "foreign_body", "Foreign body", "Foreign body",
            "A non-biological object or an insect in the canal."
        ),
        Condition(
            "bullous", "Bullous myringitis", "Bullous myringitis",
            "Blood-filled or clear blisters on the drum or deep canal."
        ),
        Condition(
            "haemotympanum", "Haemotympanum", "Haemotympanum",
            "Dark blue, purple or red drum from blood behind it, usually after trauma."
        ),
        Condition(
            "exostosis", "Exostosis / osteoma", "Exostosis",
            "Smooth, hard, pale bony swellings narrowing the canal."
        )
    )

    val other = Condition("other", "Other / not listed", "Other", "")
    private val poorQuality = Condition("poor_quality", "Image not clear enough", "Poor image", "")

    private val byId: Map<String, Condition> =
        (conditions + other + poorQuality).associateBy { it.id }

    fun name(id: String): String = byId[id]?.name ?: pretty(id)

    fun short(id: String): String = byId[id]?.short ?: pretty(id)

    fun signs(id: String): String? = byId[id]?.signs?.takeIf { it.isNotBlank() }

    fun isQualityLabel(id: String): Boolean = id == "poor_quality" || id == "unclear"

    private fun pretty(id: String): String =
        id.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
