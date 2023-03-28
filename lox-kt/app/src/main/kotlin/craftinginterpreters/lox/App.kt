package craftinginterpreters.lox

fun main(args: Array<String>) {
    when (args.size) {
        0 -> {
            Lox.runPrompt()
        }
        1 -> {
            Lox.runFile(args[0])
        }
        else -> {
            println("Usage: lox [script]")
            kotlin.system.exitProcess(64)
        }
    }
}
