package craftinginterpreters.lox

class LoxClass(
        val name: String,
        val superclass: LoxClass? = null,
        val methods: Map<String, LoxFunction> = HashMap()
) : LoxCallable {
    override val arity: Int
        get() {
            val initializer = findMethod("init")

            return initializer?.arity ?: 0
        }

    fun findMethod(name: String): LoxFunction? {
        return methods.get(name) ?: superclass?.findMethod(name)
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments)
        }
        return instance
    }

    public override fun toString(): String {
        return this.name
    }
}
