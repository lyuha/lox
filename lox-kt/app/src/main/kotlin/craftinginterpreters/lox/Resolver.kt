package craftinginterpreters.lox

class Resolver(val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    val scopes = ArrayList<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    // Stmt
    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS
        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null) {
            if (stmt.name.lexeme == stmt.superclass.name.lexeme) {
                Lox.error(stmt.superclass.name, "A class can\'t inherit from itself.")
            } else {
                currentClass = ClassType.SUBCLASS
                resolve(stmt.superclass)
            }
        }

        if (stmt.superclass != null) {
            beginScope()
            scopes.lastOrNull()?.put("super", true)
        }

        beginScope()
        scopes.lastOrNull()?.put("this", true)

        for (method in stmt.methods) {
            val declaration =
                    if (method.name.lexeme == "init") {
                        FunctionType.INITIALIZER
                    } else {
                        FunctionType.METHOD
                    }
            resolveFunction(method, declaration)
        }

        endScope()

        if (stmt.superclass != null) {
            endScope()
        }

        currentClass = enclosingClass
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)

        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can\'t return from top-level code.")
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can\'t return a value from an initializer.")
            }
            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    // Expr
    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)

        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal?) {}

    override fun visitLogicExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        when (currentClass) {
            ClassType.NONE -> Lox.error(expr.keyword, "Can\'t use \'super\' outside of a class.")
            ClassType.CLASS ->
                    Lox.error(expr.keyword, "Can\'t use \'super\' in a class with no superclass.")
            ClassType.SUBCLASS -> resolveLocal(expr, expr.keyword)
        }
    }

    override fun visitThisExpr(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can\'t use \'this\' outside of a class.")
            return
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (scopes.lastOrNull()?.get(expr.name.lexeme) == false) {
            Lox.error(expr.name, "Can\'t read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    fun resolve(statements: List<Stmt?>) {
        for (statement in statements) {
            if (statement != null) {
                resolve(statement)
            }
        }
    }

    fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun beginScope() {
        scopes.add(HashMap<String, Boolean>())
    }

    private fun endScope() {
        scopes.removeLastOrNull()
    }

    private fun declare(name: Token) {

        val scope = scopes.lastOrNull()

        if (scope == null) {
            return
        }

        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.")
        }

        scope.put(name.lexeme, false)
    }

    private fun define(name: Token) {
        scopes.lastOrNull()?.put(name.lexeme, true)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (index in (scopes.size - 1) downTo 0) {
            if (scopes.get(index).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - index)
                return
            }
        }
    }
}

private enum class FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD,
}

private enum class ClassType {
    NONE,
    CLASS,
    SUBCLASS,
}
