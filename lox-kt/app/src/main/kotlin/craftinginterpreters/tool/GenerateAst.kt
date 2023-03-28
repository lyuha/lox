package craftinginterpreters.lox.tool

import java.io.File
import java.io.IOException
import java.io.PrintWriter

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        kotlin.system.exitProcess(64)
    }

    val outputDir = args[0]

    defineAST(
            outputDir,
            "Expr",
            listOf(
                    "Binary : Expr left, Token operator, Expr right",
                    "Grouping : Expr expression",
                    "Literal : Any value",
                    "Unary : Token operator, Expr right",
            )
    )

    defineAST(outputDir, "Stmt", listOf("Expression : Expr expression", "Print : Expr expression"))
}

@Throws(IOException::class)
fun defineAST(outputDir: String, baseName: String, types: List<String>) {
    val path = "${outputDir}/${baseName}/.kt"

    val file = File(path)
    file.printWriter(Charsets.UTF_8).use { out ->
        out.println("package craftinginterpreters.lox")
        out.println()

        out.println("abstract class ${baseName} {")

        for (type in types) {
            val className = type.split(":")[0].trim()
            val fields = type.split(":")[1].trim()
            defineType(out, baseName, className, fields)
        }

        out.println("}")
    }
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    writer.println("class ${className}: ${baseName} {")
    writer.println()

    writer.println()
}
