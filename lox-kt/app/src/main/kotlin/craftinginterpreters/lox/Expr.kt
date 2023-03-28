package craftinginterpreters.lox

abstract class Expr {
    abstract fun <T> accept(visiter: Visitor<T>): T

    interface Visitor<out T> {
        // Expr
        fun visitAssignExpr(expr: Assign): T
        fun visitBinaryExpr(expr: Binary): T
        fun visitCallExpr(expr: Call): T
        fun visitGetExpr(expr: Get): T
        fun visitGroupingExpr(expr: Grouping): T
        fun visitLiteralExpr(expr: Literal?): T
        fun visitLogicExpr(expr: Logical): T
        fun visitSetExpr(expr: Set): T
        fun visitSuperExpr(expr: Super): T
        fun visitThisExpr(expr: This): T
        fun visitUnaryExpr(expr: Unary): T
        fun visitVariableExpr(expr: Variable): T
    }

    class Assign(val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitAssignExpr(this)
        }
    }

    class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitBinaryExpr(this)
        }
    }

    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitCallExpr(this)
        }
    }

    class Get(val obj: Expr, val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitGetExpr(this)
        }
    }

    class Grouping(val expression: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitGroupingExpr(this)
        }
    }

    class Literal(val value: Any?) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLiteralExpr(this)
        }
    }

    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLogicExpr(this)
        }
    }

    class Set(val obj: Expr, val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitSetExpr(this)
        }
    }

    class Super(val keyword: Token, val method: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitSuperExpr(this)
        }
    }

    class This(val keyword: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitThisExpr(this)
        }
    }

    class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitUnaryExpr(this)
        }
    }

    class Variable(val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitVariableExpr(this)
        }
    }
}
/*
class () : Expr {
    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.(this)
    }
}
*/
