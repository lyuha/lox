package craftinginterpreters.lox

class LoxFunction(
        val declaration: Stmt.Function,
        val closure: Environment,
        val isInitializer: Boolean = false
) : LoxCallable {
    override val arity: Int = declaration.params.size

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (index in 0 until declaration.params.size) {
            environment.define(declaration.params.get(index).lexeme, arguments.get(index))
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) {
                return closure.getAt(0, "this")
            }

            return returnValue.value
        }

        if (isInitializer) {
            return closure.getAt(0, "this")
        }

        return null
    }

    public override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}
