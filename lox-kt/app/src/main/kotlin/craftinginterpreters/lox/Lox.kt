package craftinginterpreters.lox

import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

object Lox {
    val interpreter = Interpreter()
    var hadError = false
    var hadRuntimeError = false

    fun runFile(path: String) {
        println(Paths.get("").toAbsolutePath().toString())
        run(File(path).readText(Charsets.UTF_8))

        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            run(line)
            hadError = false
        }
    }

    fun run(source: String) {
        val scanner = Scanner(source)
        val tokens: List<Token> = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        if (hadError) return

        val resolver = Resolver(interpreter)
        resolver.resolve(statements)

        if (hadError) return

        interpreter.interpret(statements)
    }

    fun report(line: Int, where: String, message: String) {
        System.err.println("[line ${line}] Error ${where}: ${message}")
        hadError = true
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", message)
        } else {
            report(token.line, "at \'${token.lexeme}\'", message)
        }
    }

    fun runtimeError(err: RuntimeError) {
        System.err.println(err.message)
        System.err.println("[line ${err.token.line}]")
        hadRuntimeError = true
    }
}
