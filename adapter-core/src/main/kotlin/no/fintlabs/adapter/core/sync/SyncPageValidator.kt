package no.fintlabs.adapter.core.sync

import no.fintlabs.adapter.core.logger
import no.fintlabs.adapter.models.sync.SyncPage

/**
 * Sanity checks on a batch of sync pages, run when `fint.adapter.debug` is on.
 */
class SyncPageValidator {
    private val log = logger<SyncPageValidator>()

    fun validate(
        pages: List<SyncPage>,
        resourceSize: Int,
    ): List<String> {
        val problems =
            buildList {
                if (!validPageSize(pages)) add("Page size does not match resources size")
                if (!validTotalSize(pages, resourceSize)) add("Total size does not match amount of resources")
                if (hasDuplicateIds(pages)) add("Duplicate ids found")
                if (!validIds(pages)) add("One or more ids is not a valid URN")
            }
        problems.forEach { log.warn("{}", it) }
        return problems
    }

    fun validPageSize(pages: List<SyncPage>): Boolean = pages.all { it.metadata.pageSize == it.resources.size.toLong() }

    fun validTotalSize(
        pages: List<SyncPage>,
        totalSize: Int,
    ): Boolean = pages.sumOf { it.resources.size } == totalSize

    fun hasDuplicateIds(pages: List<SyncPage>): Boolean {
        val seen = HashSet<String>()
        return pages.asSequence().flatMap { it.resources.asSequence() }.any { !seen.add(it.identifier) }
    }

    fun validIds(pages: List<SyncPage>): Boolean =
        pages.asSequence().flatMap { it.resources.asSequence() }.all { URN.matches(it.identifier) }

    companion object {
        private val URN = Regex("^urn:[a-z0-9][a-z0-9-]{1,31}:([a-z0-9()+,\\-.:=@;\$_!*']|%[0-9a-f]{2})*$")
    }
}
