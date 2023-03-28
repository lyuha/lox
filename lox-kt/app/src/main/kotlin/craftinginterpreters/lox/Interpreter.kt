package craftinginterpreters.lox

import kotlinx.datetime.Clock

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    val globals = Environment()
    var environment = globals
    val locals: MutableMap<Expr, Int> = HashMap()

    init {
        globals.define(
                "clock",
                object : LoxCallable {
                    override val arity = 0

                    override fun call(interpreter: Interpreter, arguments: List<Any?>): Long {
                        return Clock.System.now().toEpochMilliseconds()
                    }

                    public override fun toString(): String {
                        return "<native fn>"
                    }
                }
        )
    }

    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) {
                if (statement is Stmt) {
                    execute(statement)
                }
            }
        } catch (e: RuntimeError) {
            Lox.runtimeError(e)
        }
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals.put(expr, depth)
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    // Stmt

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val superclass =
                if (stmt.superclass != null) {
                    val klass = evaluate(stmt.superclass)
                    if (klass !is LoxClass) {
                        throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
                    }
                    klass
                } else {
                    null
                }

        environment.define(stmt.name.lexeme, null)

        if (stmt.superclass != null) {
            environment = Environment(environment)
            environment.define("super", superclass)
        }

        val methods = HashMap<String, LoxFunction>()

        for (method in stmt.methods) {
            val function = LoxFunction(method, environment, method.name.lexeme == "init")
            methods.put(method.name.lexeme, function)
        }

        val klass = LoxClass(stmt.name.lexeme, superclass, methods)

        if (superclass != null) {
            environment = environment.enclosing
        }

        environment.assign(stmt.name, klass)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = if (stmt.value != null) evaluate(stmt.value) else null

        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        environment.define(
                stmt.name.lexeme,
                if (stmt.initializer != null) evaluate(stmt.initializer) else null
        )
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    // Expr

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals.get(expr)
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        when (expr.operator.type) {
            // Comparison
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) <= (right as Double)
            }
            // equality
            TokenType.BANG_EQUAL -> return !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> return isEqual(left, right)
            // arithmetic
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double - right as Double
            }
            TokenType.PLUS -> {
                if (left is Double && right is Double) {
                    return left + right
                }

                if (left is String && right is String) {
                    return left + right
                }

                throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) / (right as Double)
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) * (right as Double)
            }
            else -> return null
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments = ArrayList<Any?>()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }

        val function = callee
        if (arguments.size != function.arity) {
            throw RuntimeError(
                    expr.paren,
                    "Expected ${function.arity} arguments but got ${arguments.size}."
            )
        }

        return function.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }

        throw RuntimeError(expr.name, "Only instances have properties.")
    }

    override fun visitLiteralExpr(expr: Expr.Literal?): Any? {
        return expr?.value
    }

    override fun visitLogicExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) {
                return left
            }
        } else {
            if (!isTruthy(left)) {
                return left
            }
        }

        return evaluate(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)

        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }

        val value = evaluate(expr.value)
        obj.set(expr.name, value)
        return value
    }

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals.get(expr)!!
        val superclass = environment.getAt(distance, "super") as LoxClass
        val obj = environment.getAt(distance - 1, "this") as LoxInstance

        val method = superclass.findMethod(expr.method.lexeme)

        if (method == null) {
            throw RuntimeError(expr.method, "Undefined property \'${expr.method.lexeme}\'.")
        }

        return method.bind(obj)
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            TokenType.BANG -> return !isTruthy(right)
            else -> return null
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals.get(expr)
        if (distance != null) {
            return environment.getAt(distance, name.lexeme)
        } else {
            return globals.get(name)
        }
    }

    private fun isTruthy(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj is Boolean) return obj

        return true
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        return a == b
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return

        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return

        throw RuntimeError(operator, "Operands must be enumbers.")
    }

    private fun stringify(obj: Any?): String {
        if (obj is Double) {
            val text = obj.toString()

            return if (text.endsWith(".0")) {
                text.substring(0, text.length - 2)
            } else {
                text
            }
        }

        return obj?.toString() ?: "nil"
    }
}

class RuntimeError(val token: Token, message: String) : RuntimeException(message)
