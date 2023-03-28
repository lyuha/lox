package craftinginterpreters.lox

val keywords: Map<String, TokenType> =
        mapOf(
                "and" to TokenType.AND,
                "class" to TokenType.CLASS,
                "else" to TokenType.ELSE,
                "false" to TokenType.FALSE,
                "for" to TokenType.FOR,
                "fun" to TokenType.FUN,
                "if" to TokenType.IF,
                "nil" to TokenType.NIL,
                "or" to TokenType.OR,
                "print" to TokenType.PRINT,
                "return" to TokenType.RETURN,
                "super" to TokenType.SUPER,
                "this" to TokenType.THIS,
                "true" to TokenType.TRUE,
                "var" to TokenType.VAR,
                "while" to TokenType.WHILE,
        )

class Scanner(val source: String) {
    val tokens = ArrayList<Token>()
    private var start: Int = 0
    private var current: Int = start
    private var line: Int = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        this.tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun isAtEnd(): Boolean {
        return this.current >= source.length
    }

    private fun scanToken() {
        val ch: Char = advance()
        when (ch) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)
            // Operator
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance()
                    }
                } else {
                    addToken(TokenType.SLASH)
                }
            }
            // Whitespaec
            ' ',
            '\r',
            '\t' -> {}
            '\n' -> line++
            '\"' -> string()
            else -> {
                if (isDigit(ch)) {
                    number()
                } else if (isAlpha(ch)) {
                    identifier()
                } else {
                    Lox.error(line, "Unexpecet charater.")
                }
            }
        }
    }

    private fun addToken(kind: TokenType) {
        addToken(kind, null)
    }

    private fun addToken(kind: TokenType, literal: Any?) {
        val text = this.source.substring(start, current)
        tokens.add(Token(kind, text, literal, line))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) {
            return false
        }
        if (source.get(current) != expected) {
            return false
        }

        current++
        return true
    }

    private fun advance(): Char {
        return source.get(this.current++)
    }

    private fun peek(): Char {
        return if (isAtEnd()) '\u0000' else source.get(current)
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) {
            return '\u0000'
        }
        return source.get(current + 1)
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun string() {
        while (peek() != '\"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++
            }
            advance()
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.")
            return
        }

        // The closing ".
        advance()

        val str = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, str)
    }

    private fun number() {
        while (isDigit(peek())) {
            advance()
        }

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance()

            while (isDigit(peek())) {
                advance()
            }
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) {
            advance()
        }

        val text = source.substring(start, current)
        val type = keywords.get(text) ?: TokenType.IDENTIFIER

        addToken(type)
    }

    private fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z') || c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }
}
