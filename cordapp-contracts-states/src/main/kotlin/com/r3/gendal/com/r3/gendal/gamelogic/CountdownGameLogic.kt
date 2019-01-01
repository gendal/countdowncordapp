package com.r3.gendal.com.r3.gendal.gamelogic

import net.corda.core.serialization.CordaSerializable
import java.util.*
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.abs

// A simple (brute-force) program to solve the Countdown Numbers Game,
// explained eg here: http://datagenetics.com/blog/august32014/index.html

fun main(args: Array<String>) {
    val (target, numbers)= determineNumbersAndTarget(args)

    println("The numbers are: ${numbers}")
    println("Target: ${target}\n")

    // Generate all orderings of the numbers
    // Note: this only generates lists of length n (typically 6)
    // A fuller solution would also generate all lists of _all_ lengths.
    // So this program will fail to solve problems that can only be solved by using
    // fewer than all of the available numbers.
    //
    // Check: allNumberOrderings.size will be equal to the number of permutations of numbers
    // (nPn, typically 6P6 = 720)

    val bestSolution: Value = solveCountdownNumbersGame(numbers, target)

    if (bestSolution.evaluate() == 0) {
        println("No solutions found")
    } else {
        val solution = bestSolution.evaluate()
        if (solution == target) println("Solution: ${bestSolution} = ${target}") else
            println("Closest answer: ${bestSolution} = ${solution} (${abs(solution-target)} away)")
    }
}

fun solveCountdownNumbersGame(inputList: List<Int>, target: Int): Value {

    // We calculate all perms and, for each ordering, we now brute-force the solution.
    //
    // Intuition: imagine, for a given permutation of the numbers, you insert all possible
    // combinations of +-*/ operators between
    // the numbers, such that all possible combinations of operators are tested, and such that
    // all possible precedences (ie brackets) are tested.  We do this by constructing a
    // series of Abstract Syntax Trees representing all possible combinations of operators and precedences
    //
    // In other words, we construct a series of binary trees representing the expression under construction
    // and, to achieve all possible precedences, we do this (n-1) times, with the operator between numbers 1+2
    // being the root of the AST, then the operator between numbers 2+3 and so on.
    //
    // Example:
    //
    // If the six numbers (if sorted) were [1, 2, 4, 6, 25, 100] and the current
    // permutation was, say, [2, 4, 6, 25, 100, 1]
    // then generateAllASTs will start with [2] and [4, 6, 25, 100, 1],
    // inserting, in turn, +, -, *, / as the root of the generated AST, with '2'
    // hanging off the left and an AST containing 4, 6, 25, 100 and 1 off the right.
    //
    // We then need to recurse, of course, since the AST on the right isn't yet evaluatable...
    // We have to generate all possible ASTs consisting of those five numbers, with the root of the
    // tree occurring in each of the four possible insertion points.
    //
    // In so doing, we generate all possible ASTs. And note, we don't need to reorder/permute any of the numbers
    // as we go..  the "written out" form of any ASTs generated will all contain the same numbers in the same order
    // but, of course, with lots of brackets to show operator affinity.  This is fine because the permutations
    // we calculate at the start of the program will generate every possible left-to-right ordering of
    // numbers... our job at this point is just to insert every possible combination of operators, with the
    // "try every insertion point as the root of the AST" trick enabling us to be sure of trying every
    // possible bracketing.

    var closest = Int.MAX_VALUE // print out any solutions we find on the way that are closer to the target than the best found so far
    var bestExpression: Value = Number(0)

    generatePerms(inputList).forEach {
        generateAllASTs(it.toIntArray()).forEach { expression ->
            val result = expression.evaluate()
            if (result == target) {
                return expression
            }
            if (abs(result - target) < closest) {
                closest = abs(result - target) // only print one match at any given closenesses
                bestExpression = expression
            }
        }
    }
    return bestExpression
}

fun generatePerms(list: List<Int>): Sequence<List<Int>> = buildSequence<List<Int>> {
    if (list.size == 1) yield (list) else {
        for (i in 0 until list.size) {
            val remainderList = list.toMutableList()
            val newFirstElement = list.elementAt(i)
            remainderList.removeAt(i)
            val permsOfRemainder = generatePerms(remainderList)
            permsOfRemainder.forEach {
                val newList = mutableListOf(newFirstElement)
                newList.addAll(it)
                yield(newList)
            }
        }
    }

}

fun generateAllASTs(inputList: IntArray): Sequence<Value> = buildSequence<Value> {
    // If one entry, return it.
    //
    // Otherwise, there are at least two entries in the array. We now iterate across the spaces
    // in the array, slicing it between each pair of members in turn.
    // The two slices so formed in each step will be the
    // left and right branches of an AST rooted at this point
    //
    // Example: if [1,2,3] is passed in, we will generate:
    //   [1], [2,3] and then [1, 2], [3]
    //
    // And then, for each of these, we try each root operator (+-/*) then recurse until we have a full
    // AST that can be returned (yielded) for evaluation
    if (inputList.size == 1) {
        yield(Number(inputList.elementAt(0)))
    } else {
        (1 until inputList.size).forEach { pivot ->
            val leftList = inputList.sliceArray(0 until pivot)
            val rightList = inputList.sliceArray(pivot until inputList.size)
            val lefts = generateAllASTs(leftList)
            val rights = generateAllASTs(rightList)
            lefts.forEach { left ->
                rights.forEach { right ->
                    var temp: Value
                    temp = Plus(left, right); if (temp.evaluate() >= 0) yield(temp)
                    temp = Minus(left, right); if (temp.evaluate() >= 0) yield(temp)
                    temp = Multiply(left, right); if (temp.evaluate() >= 0) yield(temp)
                    temp = Divide(left, right); if (temp.evaluate() >= 0) yield(temp)
                }
            }
        }
    }
}

fun validateCountdownSolution(proposedSolution: Value, target: Int): Boolean = (proposedSolution.evaluate()==target)

@CordaSerializable
interface Value {
    fun evaluate(): Int
}

@CordaSerializable
abstract class Operator(val left:Value, val right: Value) : Value

@CordaSerializable
class Number(val value: Int) : Value {
    override fun evaluate(): Int {
        return value
    }
    override fun toString(): String {
        return "${value}"
    }
}

@CordaSerializable
class Plus(left: Value, right: Value) : Operator(left, right) {
    override fun evaluate(): Int {
        return left.evaluate() + right.evaluate()
    }
    override fun toString(): String {
        return "${left} + ${right}"
    }
}

@CordaSerializable
class Minus(left: Value, right: Value) : Operator(left, right) {
    override fun evaluate(): Int {
        return left.evaluate() - right.evaluate()
    }
    override fun toString(): String {
        return "(${left}) - (${right})"
    }
}

@CordaSerializable
class Multiply(left: Value, right: Value) : Operator(left, right) {
    override fun evaluate(): Int {
        return left.evaluate() * right.evaluate()
    }
    override fun toString(): String {
        return "(${left}) * (${right})"
    }
}

@CordaSerializable
class Divide(left: Value, right: Value) : Operator(left, right) {
    override fun evaluate(): Int {
        val rightVal = right.evaluate()
        if (rightVal == 0) {
            // Trying to divide by zero!!
            return -10000000
        }
        val leftVal = left.evaluate()
        if (leftVal.rem(rightVal) != 0 ){
            // Not a whole number!!
            return -20000000
        }
        return leftVal.div(rightVal)
    }
    override fun toString(): String {
        return "(${left}) / (${right})"
    }
}

private fun determineNumbersAndTarget(args: Array<String>): Pair<Int, List<Int>> {
    if (args.size < 2) {
        println("Usage: CountdownSolver target numbers (at least one)")
        System.exit(-1)
    }
    var target = args[0].toIntOrNull()
    if (target == null) {
        target = Random().nextInt(1000)
        println("Could not parse target (${args[0]}). Selecting random target instead")
    }
    var random = false
    val workingNumbers = ArrayList<Int>()
    for (i in 1 until args.size) {
        val temp = args[i].toIntOrNull()
        if (temp == null) {
            println("  Could not parse number (${args[i]}). Selecting six random numbers instead")
            random = true
            break
        }
        workingNumbers.add(temp)
    }
    val numbers = if (random) generateNumbers() else workingNumbers.toList()
    return Pair(target, numbers)
}

fun generateNumbers(): List<Int> {
    val list = mutableListOf<Int>()
    val random = Random()
    // Simulate the decision of how many 'large' numbers to select
    val largeNumbers = random.nextInt(5)
    val smallNumbers = 6 - largeNumbers
    (1..largeNumbers).forEach {
        // Note: this doesn't fully match real gameplay as it could select the same number twice
        list += 25 + random.nextInt(4) * 25
    }
    (1..smallNumbers).forEach {
        list += 1 + random.nextInt(9)
    }
    return list
}
