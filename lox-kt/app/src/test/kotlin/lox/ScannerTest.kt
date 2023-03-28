package craftinginterpreters.lox

import kotlin.test.Test
import kotlin.test.assertEquals

class ScannerTest {
        @Test
        fun `scan '1 + 2'`() {
                val source = "1 + 2"
                val scanner = Scanner(source)

                val expect =
                                listOf(
                                                Token(TokenType.NUMBER, "1", 1.0, 1),
                                                Token(TokenType.PLUS, "+", null, 1),
                                                Token(TokenType.NUMBER, "2", 2.0, 1),
                                                Token(TokenType.EOF, "", null, 1),
                                )
                assertEquals(scanner.scanTokens(), expect)
        }

        @Test
        fun `scan '== != + a !'`() {
                val source = "== != + a !"
                val scanner = Scanner(source)

                val expect =
                                listOf(
                                                Token(TokenType.EQUAL_EQUAL, "==", null, 1),
                                                Token(TokenType.BANG_EQUAL, "!=", null, 1),
                                                Token(TokenType.PLUS, "+", null, 1),
                                                Token(TokenType.IDENTIFIER, "a", null, 1),
                                                Token(TokenType.BANG, "!", null, 1),
                                                Token(TokenType.EOF, "", null, 1),
                                )
                assertEquals(scanner.scanTokens(), expect)
        }

        @Test
        fun `scan 'print "one"'`() {
                val source = "print \"one\";"
                val scanner = Scanner(source)

                assertEquals(
                                scanner.scanTokens(),
                                listOf(
                                                Token(TokenType.PRINT, "print", null, 1),
                                                Token(TokenType.STRING, "\"one\"", "one", 1),
                                                Token(TokenType.SEMICOLON, ";", null, 1),
                                                Token(TokenType.EOF, "", null, 1)
                                )
                )
        }
}
