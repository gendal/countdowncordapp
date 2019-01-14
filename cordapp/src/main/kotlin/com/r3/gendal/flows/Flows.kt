package com.r3.gendal.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gendal.com.r3.gendal.gamelogic.Number
import com.r3.gendal.contracts.CountdownContract
import com.r3.gendal.states.CountdownState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class ProposeChallenge(val target: Int,
                       val gameTiles: List<Int>,
                       val otherParty: Party) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        println("Propose Challenge Flow initiated with target: ${target}, gameTiles: ${gameTiles}, otherParty: ${otherParty}")

        val gameState: CountdownState = CountdownState(target, gameTiles, Number(0), false)
        val txCommand = Command(CountdownContract.Commands.Challenge(), listOf(serviceHub.myInfo.legalIdentities[0].owningKey, otherParty.owningKey))
        val notary: Party = serviceHub.networkMapCache.notaryIdentities[0]
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(gameState, CountdownContract.ID)
                .addCommand(txCommand)
        txBuilder.verify(serviceHub)
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)
        val otherPartyFlow = initiateFlow(otherParty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))
        val finalisedTx = subFlow(FinalityFlow(fullySignedTx, setOf(otherPartyFlow)))
        return finalisedTx
    }
}

@InitiatedBy(ProposeChallenge::class)
class ChallengeResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        println("Challenge Responder flow started")

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: Check we're happy to sign....
                val gameState = stx.tx.outputsOfType(CountdownState::class.java).single()
                println("Received gameState: ${gameState} in Responder flow")
                // see: https://github.com/corda/cordapp-example/blob/release-V4/kotlin-source/src/main/kotlin/com/example/flow/ExampleFlow.kt
            }
        }

        val txId = subFlow(signTransactionFlow).id
        val finalisedTx = subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
        println("Finalised transaction returned")

        return finalisedTx

    }
}
