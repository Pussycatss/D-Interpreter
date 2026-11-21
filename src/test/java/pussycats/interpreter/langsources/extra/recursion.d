// recursion through a variable (is it in scope inside its own initializer?)
var fact := func(n) is
  if n <= 1 then return 1 end
  return n * fact(n - 1)
end
print fact(10)

// closure capturing a variable, plus mutation
var makeCounter := func() is        // grammar forbids empty parens: should this work?
  var c := 0
//  return func => c := c + 1         // assignment is not an Expression, so this is a syntax error
end

var mk := func(start) is
  var c := start
  return func(d) is c := c + d; return c end
end
var ctr := mk(10)
print ctr(1), ctr(1)   // 11, 12
var ctr2 := mk(10)
print ctr2(1)          // 11, independent of ctr

// functions as values in containers
var t := {f := func(x) => x * 2, g := func(x) => x + 1}
print t.f(t.g(3))      // 8

var a := []
a[100] := func(x) => x + 1
print a[100](5)

// immediately invoked: Reference doesn't derive from Primary, so test it anyway
print (func(x) => x + 1)(2)

// wrong arity
var f := func(a, b) => a + b
print f(1)
print f(1, 2, 3)

// function with no return value used in an expression
var g := func(x) is print x end
print g(1) + 1         // none + 1: error

// functions in operators
print f + f
print f = f
print f is func