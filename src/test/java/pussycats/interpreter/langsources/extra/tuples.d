var t := {a := 1, b := 2, 3}
print t.b, t.2, t.3
print t.4              // out of range: error
print t.z              // missing name: error
print t.0              // numbering starts at 1

t := t + {e := 5}
print t.e, t.4

// duplicate names after concatenation: names must be unique
print {a := 1} + {a := 2}

// can a tuple's values change? "structure cannot be modified"
t.a := 100
t.3 := 300
//t.zz := 1              // adding a new name must fail

// edge shapes
var e := {}            // empty tuple
print e is {}
print {1}              // single element: tuple or parenthesised value?
print {a := {b := {c := 42}}}.a.b.c

// {x := 1, 2} vs a lexer reading {x := 1} followed by 2
var n := 5
var t2 := {n, n := 7}  // first element is the expression n, second is named n

// a named element whose value is a call result
var f := func(x) => {v := x}
print f(3).v

// elements must be accessible by index in a loop
for x in {1, "two", 3.0, [4]} loop print x end

// tuple equality
print {1, 2} = {1, 2}  // comparisons only allow numbers per the table