package com.r3.gendal.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.gendal.com.r3.gendal.gamelogic.Number
import com.r3.gendal.contracts.CountdownContract
import com.r3.gendal.states.CountdownState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class ProposeChallenge(val target: Int,
                       val gameTiles: List<Int>,
                       val otherParty: Party) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        println("Propose Challenge Flow initiated with target: ${target}, gameTiles: ${gameTiles}, otherParty: ${otherParty}")
        val notary: Party = serviceHub.networkMapCache.notaryIdentities[0]

        val gameState: CountdownState = CountdownState(target, gameTiles, Number(0), false)

        /*
        val txCommand = Command(CountdownContract.Commands.Challenge(), serviceHub.myInfo.legalIdentities[0].owningKey)


        val txBuilder = TransactionBuilder(notary)
            .addOutputState(gameState, CountdownContract.ID)
            .addCommand(txCommand)

        txBuilder.verify(serviceHub)

        */

        //val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        val otherPartyFlow = initiateFlow(otherParty)

        //val fullySignedTx = subFlow(CollectSignatureFlow(partSignedTx, otherPartyFlow))

        val resultUnsafe: UntrustworthyData<String> = otherPartyFlow.sendAndReceive<String>(gameState)

        //println("Result: ${resultUnsafe.unwrap{ data -> data}}")

        val result = resultUnsafe.unwrap { data -> data }

        println("Result: ${result}")

        return result

    }
}


@InitiatedBy(ProposeChallenge::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        // Responder flow logic goes here.
        println("Responder flow started")
        val untrustworthyData = counterpartySession.receive<CountdownState>()
        val gameState = untrustworthyData.unwrap { gameState -> gameState }
        println("Received gameState: ${gameState} in responding flow")
        counterpartySession.send("Game state OK")
        return("Game state acknowledgement sent by response to initiator. Responder ending.")

    }
}
