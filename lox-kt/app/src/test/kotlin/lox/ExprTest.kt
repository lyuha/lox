package craftinginterpreters.lox

import kotlin.test.Test
import kotlin.test.assertEquals

class ExprTest {
    @Test
    fun expr() {
        val expression =
                Expr.Binary(
                        Expr.Unary(Token(TokenType.MINUS, "-", null, 1), Expr.Literal(123)),
                        Token(TokenType.STAR, "*", null, 1),
                        Expr.Grouping(Expr.Literal(45.67))
                )

        assertEquals(AstPrinter().print(expression), "(* (- 123) (group 45.67))")
    }
}
