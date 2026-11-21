var t := []
t[10] := 25
t[100] := func(x) => x + 1
t[1000] := {a := 1, b := 2.7}
print t[1000].b
print t[1]             // 1 is unset: none or error?
print t[0]             // 0 is unset: none or error?
t[-5] := 1             // negative keys allowed? the spec says only "integer"

// iteration order over a sparse array
for v in t loop print v end   // insertion order or key order?

// concatenation of sparse arrays: are keys renumbered?
var p := [1, 2]
var s := []
s[5] := 9
print p + s            // what are the resulting keys?

// index types
var m := [1, 2, 3]
print m[1.0]           // real index
print m["a"]           // string key? "real associative array with integer keys"
print m[true]

// reference vs value semantics
var a := [1, 2]
var b := a
b[1] := 99
print a[1]             // 99 if reference, 1 if copy; the spec doesn't say

// self-containing array: should not hang in print
var c := [1]
c[2] := c
print c                // infinite recursion if print isn't cycle-safe

// nesting and chaining
var grid := [[1, 2], [3, 4]]
grid[2][1] := 30
print grid[2][1]
var d := [[1]]
d[1][5][2] := 1        // auto-vivification of the missing d[1][5]?
print d