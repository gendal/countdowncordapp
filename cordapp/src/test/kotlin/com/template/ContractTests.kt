package com.r3.gendal

import com.r3.gendal.com.r3.gendal.gamelogic.Number
import com.r3.gendal.com.r3.gendal.gamelogic.solveCountdownNumbersGame
import com.r3.gendal.contracts.CountdownContract
import com.r3.gendal.states.CountdownState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.EnforceVerifyOrFail
import net.corda.testing.dsl.TransactionDSL
import net.corda.testing.dsl.TransactionDSLInterpreter
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Before
import org.junit.Test

class IOUFlowTests {

    lateinit var PartyA: TestIdentity
    lateinit var PartyB: TestIdentity
    lateinit var DummyNotary: TestIdentity

    private val inState = CountdownState(
        numberTiles = listOf(1, 2, 3, 4, 75, 100),
        target = 555,
        proposedSolution = Number(0),
        gameSolved = false
    )

    private val outState = inState.copy(
        gameSolved = true
    )

    @Before
    fun setup() {
        PartyA = TestIdentity(CordaX500Name("PartyA", "London", "GB"))
        PartyB = TestIdentity(CordaX500Name("PartyB", "New York", "US"))
        DummyNotary = TestIdentity(CordaX500Name("Dummy Notary", "Paris", "FR"))
    }

    private fun transaction(script: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail) {
        MockServices(PartyA).transaction(DummyNotary.party , script)
    }

    @Test
    fun trivial() {
        transaction {
            attachment(CountdownContract.ID)

            tweak {
                // A valid output-only transaction containing a valid challenge
                output(CountdownContract.ID, inState)
                command(PartyA.publicKey, CountdownContract.Commands.Challenge())
                this.verifies()
            }
            tweak {
                // An input-only transaction with an invalid challenge
                output(CountdownContract.ID, inState.copy(target = 1))
                command(PartyA.publicKey, CountdownContract.Commands.Challenge())
                this `fails with` "the target must be between 100 and 999 (inclusive)"
            }
            tweak {
                output(CountdownContract.ID, inState.copy(gameSolved = true))
                command(PartyA.publicKey, CountdownContract.Commands.Challenge())
                this `fails with` "there must not be a solution until challenge is verified and accepted"
            }
            tweak {
                input(CountdownContract.ID, inState)
                output(CountdownContract.ID, outState)
                command(PartyA.publicKey, CountdownContract.Commands.Solution())
                this `fails with` "the proposed solution must evaluate to the target"
            }
            tweak {
                input(CountdownContract.ID, inState)
                output(CountdownContract.ID, outState.copy(proposedSolution = solveCountdownNumbersGame(inState.numberTiles, inState.target)))
                command(PartyA.publicKey, CountdownContract.Commands.Solution())
                this.verifies()
            }
        }
    }
}