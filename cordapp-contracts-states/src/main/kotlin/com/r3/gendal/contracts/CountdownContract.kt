package com.r3.gendal.contracts

import com.r3.gendal.com.r3.gendal.gamelogic.Number
import com.r3.gendal.states.CountdownState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CountdownContract: Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.r3.gendal.contracts.CountdownContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.single()

        val inputs = tx.inputsOfType(CountdownState::class.java)
        val outputs = tx.outputsOfType(CountdownState::class.java)

        when (command.value) {
            is Commands.Challenge -> {
                // Must be just one input
                // The values must be valid per rules of Countdown
                if (inputs.size != 0) throw IllegalStateException("there must be no input for a challenge")
                if (outputs.size != 1) throw IllegalStateException("there must be precisely one output for a challenge")

                requireThat {
                    "there must not be a solution until challenge is verified and accepted" using
                            (outputs[0].gameSolved==false && outputs[0].proposedSolution.evaluate()==0)
                    "the target must be between 100 and 999 (inclusive)" using
                            (outputs[0].target>=100 && outputs[0].target<1000)
                    // TODO
                    //
                    // Check numbers drawn (with no replacement) from set 1 1 2 2 3 3 4 4 5 5 6 6 25 50 75 100

                }
            }

            is Commands.Solution -> {

                if (inputs.size != 1) throw IllegalStateException("there must be precisely one input for a solution")
                if (outputs.size != 1) throw IllegalStateException("there must be precisely one output for a challenge")

                requireThat {
                    "the key details must not have switched from challenge to solution" using
                            (inputs[0].target == outputs[0].target && inputs[0].numberTiles == outputs[0].numberTiles)
                                    // TODO check the equals() operator works the way I hope it does)
                    "the proposed solution must evaluate to the target" using
                            (outputs[0].proposedSolution.evaluate() == outputs[0].target)
                    "the problem must be marked as solved" using
                            (outputs[0].gameSolved)
                }
            }

            else -> throw IllegalArgumentException("command must be 'Challenge' or 'Solution'")
        }

    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Challenge : Commands
        class Solution : Commands
    }
}