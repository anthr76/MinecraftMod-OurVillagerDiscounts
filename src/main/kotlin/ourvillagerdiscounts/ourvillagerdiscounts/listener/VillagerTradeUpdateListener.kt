package ourvillagerdiscounts.ourvillagerdiscounts.listener

import java.util.Comparator
import java.util.stream.Stream
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.village.VillagerData
import net.minecraft.village.VillagerGossipType
import net.minecraft.village.VillagerGossips
import net.minecraft.village.VillagerGossips.GossipEntry
import net.minecraft.village.VillagerProfession
import org.apache.logging.log4j.LogManager
import ourvillagerdiscounts.ourvillagerdiscounts.callback.VillagerInteractCallback
import ourvillagerdiscounts.ourvillagerdiscounts.mixin.VillagerGossipEntriesInvoker

class VillagerTradeUpdateListener : VillagerInteractCallback {

    override fun interact(player: PlayerEntity, villager: VillagerEntity): ActionResult {
        val data: VillagerData = villager.villagerData
        val professionEntry = data.profession
        val gossip: VillagerGossips = villager.gossip

        // Only proceed if the villager is not NONE or NITWIT
        if (!professionEntry.matchesKey(VillagerProfession.NONE)
            && !professionEntry.matchesKey(VillagerProfession.NITWIT)) {

            // Get all gossip entries for this villager
            val gossipAccessor: Stream<GossipEntry> =
                (gossip as VillagerGossipEntriesInvoker).invokeEntries()

            // Find highest-value MAJOR_POSITIVE gossip
            gossipAccessor
                .filter { entry -> entry.type == VillagerGossipType.MAJOR_POSITIVE }
                .max(Comparator.comparingInt { it.value })
                .ifPresent { maxEntry ->
                    val majorPositiveGossipWeighted = maxEntry.value

                    // How much 'MAJOR_POSITIVE' gossip this villager has for the player
                    val currentPlayerMajorPositive =
                        gossip.getReputationFor(player.uuid) {
                            it == VillagerGossipType.MAJOR_POSITIVE
                        }

                    // If the best known major-positive gossip is bigger than what we currently have for the player...
                    if (majorPositiveGossipWeighted > currentPlayerMajorPositive) {
                        // Convert that "weighted" total to the "unweighted" value
                        // (the old code stored unweighted in NBT).
                        val majorPositiveGossipUnweighted =
                            majorPositiveGossipWeighted / maxEntry.type.multiplier

                        // Instead of manual NBT, just start (or update) new gossip directly:
                        // If you prefer to store the bigger weighted value, pass `majorPositiveGossipWeighted`.
                        // If you want a smaller unweighted #, pass `majorPositiveGossipUnweighted`.
                        gossip.startGossip(
                            player.uuid,
                            VillagerGossipType.MAJOR_POSITIVE,
                            majorPositiveGossipUnweighted
                        )
                    }
                }
        }
        return ActionResult.PASS
    }

    companion object {
        private val LOG = LogManager.getLogger(VillagerTradeUpdateListener::class.java)

        // Kept for reference if you need them, but we no longer store them in NBT:
        private const val TARGET = "Target"
        private const val TYPE = "Type"
        private const val VALUE = "Value"
    }
}
