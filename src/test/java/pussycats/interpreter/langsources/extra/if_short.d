// "=>" is used by both IfShort and FunBody
if true => print func(x) => x + 1 is func

var h := func(a) => a
if h(1) = 1 => print "ok"

// IfShort has no 'end', so its Body greedily swallows the rest of the block
var n := 0
if false => n := 1
n := 2             // is this inside the if? the grammar says yes
print n            // 0 if greedy, 2 otherwise

// nested short ifs
if true => if false => print "a"
print "b"