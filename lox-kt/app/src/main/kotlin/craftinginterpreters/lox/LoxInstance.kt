package craftinginterpreters.lox

class LoxInstance(val klass: LoxClass) {
    val fields = HashMap<String, Any?>()

    fun get(name: Token): Any? {
        if (fields.contains(name.lexeme)) {
            return fields.get(name.lexeme)
        }

        return klass.findMethod(name.lexeme)?.bind(this)
                ?: throw RuntimeError(name, "Undefined property \'${name.lexeme}\'.")
    }

    fun set(name: Token, value: Any?) {
        fields.put(name.lexeme, value)
    }

    public override fun toString(): String {
        return "${klass.name} instance"
    }
}
