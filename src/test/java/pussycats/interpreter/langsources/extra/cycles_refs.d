// Self-referencing array
var a := [1, 2]
a[3] := a
print "Cyclic array:", a
print "Nested access:", a[3][1], a[3][2]

// Mutual recursion between two arrays
var x := [10]
var y := [20, x]
x[2] := y
print "Mutual cycle X:", x
print "Mutual cycle Y:", y

// Array inside a tuple inside the same array
var container := [100]
var wrapper := {id := 1, ref := container}
container[2] := wrapper
print "Tuple-Array cycle:", wrapper