package com.r3.gendal.states

import com.r3.gendal.com.r3.gendal.gamelogic.Number
import com.r3.gendal.com.r3.gendal.gamelogic.Value
import com.r3.gendal.contracts.CountdownContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(CountdownContract::class)
data class CountdownState(
        val target: Int = 0,
        val numberTiles: List<Int>,
        val proposedSolution: Value = Number(0),
        val gameSolved: Boolean = false,
        override val participants: List<AbstractParty> = listOf()
) : ContractState
