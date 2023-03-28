package craftinginterpreters.lox

// class PsrserTest {
//     @Test
//     fun `parse '1 + 2'`() {
//         val tokens =
//                 listOf(
//                         Token(TokenType.NUMBER, "1", 1.0, 1),
//                         Token(TokenType.PLUS, "+", null, 1),
//                         Token(TokenType.NUMBER, "2", 2.0, 1),
//                         Token(TokenType.EOF, "", null, 1),
//                 )

//         val parser = Parser(tokens)

//         assertEquals(
//                 parser.parse(),
//                 Binary(Literal(1.0), Token(TokenType.PLUS, "+", null, 1), Literal(2.0))
//         )
//     }

//     @Test
//     fun `parse '== != + a !'`() {
//         val tokens =
//                 listOf(
//                         Token(TokenType.EQUAL_EQUAL, "==", null, 1),
//                         Token(TokenType.BANG_EQUAL, "!=", null, 1),
//                         Token(TokenType.PLUS, "+", null, 1),
//                         Token(TokenType.IDENTIFIER, "a", null, 1),
//                         Token(TokenType.BANG, "!", null, 1),
//                         Token(TokenType.EOF, "", null, 1),
//                 )

//         val parser = Parser(tokens)

//         assertEquals(parser.parse(), null)
//     }
// }
