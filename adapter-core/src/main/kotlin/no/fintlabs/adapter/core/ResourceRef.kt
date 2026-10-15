package no.fintlabs.adapter.core

import no.novari.fint.core.model.FintModel
import no.novari.fint.core.model.FintResourceRef

/**
 * A FINT resource by domain, package and name, e.g. personvern/samtykke/behandling.
 */
data class ResourceRef(
    val domainName: String,
    val packageName: String,
    val resourceName: String,
) {
    /** `/personvern/samtykke/behandling`, the path the gateway uses for sync and events. */
    val entityUri: String get() = "/$domainName/$packageName/$resourceName"

    /** Whether the FINT information model knows this resource, checked the way the gateway checks a contract. */
    val isKnown: Boolean get() = FintResourceRef(domainName, packageName, resourceName) in FintModel.refs

    override fun toString(): String = "$domainName/$packageName/$resourceName"

    companion object {
        private const val MODEL_PACKAGE = "no.novari.fint.model.resource."
        private const val CLASS_SUFFIX = "Resource"

        /**
         * Derives the reference from a generated model class: the package below `no.novari.fint.model.resource`
         * gives domain and package, the class name without `Resource` gives the name.
         */
        fun of(type: Class<*>): ResourceRef {
            val packageName = type.packageName
            require(packageName.startsWith(MODEL_PACKAGE) && type.simpleName.endsWith(CLASS_SUFFIX)) {
                "${type.name} is not a generated FINT resource (expected ${MODEL_PACKAGE}<domain>.<package>.<Name>$CLASS_SUFFIX); override ref"
            }
            val segments = packageName.removePrefix(MODEL_PACKAGE).split('.')
            require(segments.size == 2) { "${type.name} is not in a <domain>.<package> package; override ref" }
            return ResourceRef(segments[0], segments[1], type.simpleName.removeSuffix(CLASS_SUFFIX).lowercase())
        }
    }
}
