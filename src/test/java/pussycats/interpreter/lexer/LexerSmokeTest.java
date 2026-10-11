package pussycats.interpreter.lexer;

import java.util.List;

public class LexerSmokeTest {
    static void main() {
        System.out.println("=== Test 1: Valid code ===");
        String source =
                """
                    var i := 0
                    loop // Infinite loop
                    print "Hello"
                    i := i + 1
                    if i=100 => exit
                    end
                    
                    var t := [];
                    t[10] := 25;
                    var x := t.2
                    var pi := 3.14
                    var tup := {a:=1, b:=2.7}
                    for i in [1,2,3] loop
                      print i
                    end
                    """;

        try {
            List<Token> tokens = new Lexer(source).scan();
            for (Token t : tokens) {
                System.out.println(t);
            }
        } catch (LexerException e) {
            System.out.println(e);
        }

        System.out.println("\n=== Test 2: Unterminated string (error display) ===");
        String errorSource = "var x := \"unterminated string\n";
        
        try {
            new Lexer(errorSource).scan();
        } catch (LexerException e) {
            System.out.println(e);
        }

        System.out.println("\n=== Test 3: Unexpected character ===");
        String errorSource2 = "var x := @invalid\n";
        
        try {
            new Lexer(errorSource2).scan();
        } catch (LexerException e) {
            System.out.println(e);
        }
    }
}