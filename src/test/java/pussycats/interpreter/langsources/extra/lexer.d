// '..' vs real literal: a naive lexer reads "1." then ".3"
for 1..3 loop print "x" end

// tuple access vs real literal: "t.1.2" may lex as t, then real 1.2
var t := {{10, 20}, 30}
print t.1.2        // expected 20

var r := 1.5
print r.1          // runtime error: r is not a tuple, not a lexer crash

// quotes inside strings
print 'say "hi"', "it's"
print ''           // empty string