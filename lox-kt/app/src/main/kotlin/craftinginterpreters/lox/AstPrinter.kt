package craftinginterpreters.lox

// class AstPrinter : Expr.Visitor<String> {
//     fun print(expr: Expr): String {
//         return expr.accept<String>(this)
//     }

//     override fun visitAssignExpr(expr: Expr.Assign): String {
//         TODO("")
//     }

//     override fun visitBinaryExpr(expr: Expr.Binary): String {
//         return parenthesize(expr.operator.lexeme, expr.left, expr.right)
//     }

//     override fun visitCallExpr(expr: Expr.Call): String {
//         TODO()
//     }

//     override fun visitGroupingExpr(expr: Expr.Grouping): String {
//         return parenthesize("group", expr.expression)
//     }

//     override fun visitLiteralExpr(expr: Expr.Literal?): String {
//         return expr?.value?.toString() ?: "nil"
//     }

//     override fun visitLogicExpr(expr: Expr.Logical): String {
//         TODO()
//     }

//     override fun visitUnaryExpr(expr: Expr.Unary): String {
//         return parenthesize(expr.operator.lexeme, expr.right)
//     }

//     override fun visitVariableExpr(expr: Expr.Variable): String {
//         TODO("")
//     }

//     fun parenthesize(name: String, vararg exprs: Expr): String {
//         val builder = StringBuilder()

//         builder.append('(').append(name)
//         for (expr in exprs) {
//             builder.append(" ${expr.accept(this)}")
//         }
//         builder.append(')')

//         return builder.toString()
//     }

//     fun parenthesize(name: String, vararg parts: Any): String {
//         val builder = StringBuilder()

//         builder.append('(').append(name)

//         for (part in parts) {}

//         builder.append(')')

//         return builder.toString()
//     }
// }
