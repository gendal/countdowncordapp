package com.r3.gendal

import com.r3.gendal.flows.ProposeChallenge
import com.r3.gendal.flows.Responder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.internal.cordappsForPackages
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = cordappsForPackages("com.r3.gendal.contracts", "com.r3.gendal.flows")))
        a = network.createPartyNode()
        b = network.createPartyNode()

        /*
            TODO: Check if this is needed... I get a warning when it's included

                // For real nodes this happens automatically, but we have to manually register the flow for tests.
                // listOf(a, b).forEach { it.registerInitiatedFlow(Responder::class.java) }
        */

        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `drive simple test`() {
        val flow = ProposeChallenge(555, listOf(1,2, 3, 4, 75, 100), b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val result = future.getOrThrow()
        println("Completed: ${result}")
    }
}