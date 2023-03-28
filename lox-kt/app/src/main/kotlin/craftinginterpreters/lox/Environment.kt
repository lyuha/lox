package craftinginterpreters.lox

/*
key = value or null
*/

class Environment() {
    val values = HashMap<String, Any?>()
    lateinit var enclosing: Environment

    constructor(enclosing: Environment) : this() {
        this.enclosing = enclosing
    }

    fun get(name: Token): Any? {
        return values.getOrElse(
                name.lexeme,
                {
                    if (::enclosing.isInitialized) {
                        enclosing.get(name)
                    } else {
                        throw RuntimeError(name, "undefined variable \'${name.lexeme}\'.")
                    }
                }
        )
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value)
        } else if (::enclosing.isInitialized) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable \'${name.lexeme}\'.")
        }
    }

    fun define(name: String, value: Any?) {
        values.put(name, value)
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values.get(name)
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values.put(name.lexeme, value)
    }

    fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0 until distance) {
            environment = environment.enclosing
        }
        return environment
    }
}
