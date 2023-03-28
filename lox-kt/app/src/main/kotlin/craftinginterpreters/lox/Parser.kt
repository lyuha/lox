package craftinginterpreters.lox

import java.util.ArrayList

class Parser(val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<Stmt?> {
        val statements = ArrayList<Stmt?>()
        while (!isAtEnd()) {
            statements.add(declaration())
        }
        return statements
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.CLASS)) {
                return classDeclaration()
            } else if (match(TokenType.FUN)) {
                return function("function")
            } else if (match(TokenType.VAR)) {
                return varDeclaration()
            } else {
                return statement()
            }
        } catch (e: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")

        val superclass =
                if (match(TokenType.LESS)) {
                    consume(TokenType.IDENTIFIER, "Expect superclass name.")
                    Expr.Variable(previous())
                } else {
                    null
                }

        consume(TokenType.LEFT_BRACE, "Expect \'{\' before class body.")

        val methods = ArrayList<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expect \'}\' after class body.")

        return Stmt.Class(name, superclass, methods)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }

        consume(TokenType.SEMICOLON, "Expect \';\' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(TokenType.FOR) -> forStatement()
            match(TokenType.IF) -> ifStatement()
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after \'for\'.")

        val initializer =
                when {
                    match(TokenType.SEMICOLON) -> null
                    match(TokenType.VAR) -> varDeclaration()
                    else -> expressionStatement()
                }

        var condition = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON, "Expect \';\' after loop condition.")

        val increment = if (!check(TokenType.RIGHT_PAREN)) expression() else null
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after for clauses.")

        var body = statement()

        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        if (condition == null) {
            condition = Expr.Literal(true)
        }

        body = Stmt.While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after \'while\'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after condition.")

        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after \'if\'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after if condition.")

        val thenBranch = statement()
        val elseBranch =
                if (match(TokenType.ELSE)) {
                    statement()
                } else {
                    null
                }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect \';\' after value.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) expression() else null

        consume(TokenType.SEMICOLON, "Expect \';\' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect \';\'; after expression.")
        return Stmt.Expression(expr)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect ${kind} name.")
        consume(TokenType.LEFT_PAREN, "Expect \'(\' after ${kind} name.")

        val parameters = ArrayList<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can\'t have more than 255 parameters.")
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect \')\' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expect \'{\' before ${kind} body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun block(): List<Stmt> {
        var statements = ArrayList<Stmt?>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expect \'}\' after block.")
        return statements.filterNotNull()
    }

    // Expr

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val token = expr.name
                return Expr.Assign(token, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            } else {
                error(equals, "Invalid assignment target.")
            }
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS,
                TokenType.LESS_EQUAL
        )) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        } else {
            return call()
        }
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after \'.\'.")
                expr = Expr.Get(expr, name)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = ArrayList<Expr>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can\'t have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect \')\' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        return when {
            match(TokenType.FALSE) -> Expr.Literal(false)
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
            match(TokenType.SUPER) -> {
                val keyword = previous()
                consume(TokenType.DOT, "Expect \'.\' after \'super\'.")
                val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.")
                Expr.Super(keyword, method)
            }
            match(TokenType.THIS) -> Expr.This(previous())
            match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect \')\'  after expressiono.")
                return Expr.Grouping(expr)
            }
            else -> throw error(peek(), "Expect expression.")
        }
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) {
            return advance()
        }

        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }

        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(): Token {
        return tokens.get(current)
    }

    private fun previous(): Token {
        return tokens.get(current - 1)
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return
            }

            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> {
                    return
                }
                else -> advance()
            }
        }
    }

    class ParseError : RuntimeException()
}
