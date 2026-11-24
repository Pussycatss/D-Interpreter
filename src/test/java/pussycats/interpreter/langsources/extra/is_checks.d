print 1 is int, 1.0 is real, "s" is string, true is bool
print 1 is real        // false: no implicit widening for 'is'
print [] is []
print {} is {}
print [1] is {}
print (func => 1) is func

var x := 5
print x is int and x is real   // combined with 'and'
print not x is int             // 'not' applies to Primary, but x is a Reference
print (1 + 2) is int           // grammar allows 'is' only after a Reference