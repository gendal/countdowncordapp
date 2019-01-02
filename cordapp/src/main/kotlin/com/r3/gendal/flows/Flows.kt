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

            TODO: Submit the challenge to the other side as a Challenge transaction
                val txCommand = Command(CountdownContract.Commands.Challenge(), serviceHub.myInfo.legalIdentities[0].owningKey)

                val txBuilder = TransactionBuilder(notary)
                    .addOutputState(gameState, CountdownContract.ID)
                    .addCommand(txCommand)

                txBuilder.verify(serviceHub)

                val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        */

        val otherPartyFlow = initiateFlow(otherParty)

        /*
            TODO: Send using in-built flows
                val fullySignedTx = subFlow(CollectSignatureFlow(partSignedTx, otherPartyFlow))
                // check Participants are correct
         */

        val resultUnsafe: UntrustworthyData<String> = otherPartyFlow.sendAndReceive<String>(gameState)
        val result = resultUnsafe.unwrap { data ->

            // TODO: Validate properly

            data
        }

        println("Result from responder: ${result}")

        otherPartyFlow.send(5)
        for (i in 1..5)
            otherPartyFlow.send("$i")

        return result

    }
}

@InitiatedBy(ProposeChallenge::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        println("Responder flow started")
        val untrustworthyData = counterpartySession.receive<CountdownState>()
        val gameState = untrustworthyData.unwrap { gameState ->

            // TODO: Validate properly

            gameState
        }

        println("Received gameState: ${gameState} in responding flow")

        // TODO: Verify that the proposal is correct and sign
        counterpartySession.send("Game state OK")

        val count = counterpartySession.receive<Int>().unwrap { count -> count }
        println("I am informed I am to receive ${count} more messages")
        for (i in 1 .. count) {
            println("Waiting to receive item ${i}")
            counterpartySession.receive<Any>()
        }


        return("Game state acknowledgement sent by response to initiator. Responder ending.")

    }
}
