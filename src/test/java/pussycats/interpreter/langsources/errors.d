// 1. '=' used instead of ':=' in a variable declaration
var x = 42

// 2. Misspelled keyword at the start of a statement
pritn "hello"

// 3. 'exit' outside a loop
exit

// 4. '=' instead of ':=' inside a tuple literal
var tup := {a = 1, b := 2}

// 5. Misspelled type indicator after 'is'
var h := x is integer

// 6. Two statements on one line, no ';' separator
var i := 5 var j := 6

// 7. Unclosed '[' in an array literal
var arr := [1, 2, 3

// 8. Unclosed '(' in a parenthesized expression
var bad := (1 + 2 * 3

// 9. Unclosed '(' in a call's argument list
var k := foo(1, 2

// 10. Parameter name must be an identifier
var f := func(1, 2) => 3

// 11. Function literal missing '=>' or 'is'
var g := func(n)

// These lines are correct, for contrast.
var ok := 42
print ok