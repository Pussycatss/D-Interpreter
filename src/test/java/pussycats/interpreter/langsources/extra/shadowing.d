var x := 1
//var x := 2         // error: duplicate in the same scope

var y := 1
for 1..2 loop
  var y := 100     // legal, hides the outer y
  y := y + 1
end
print y            // 1

var i := 99
for i in 1..3 loop print i end   // loop var shadows
print i            // 99

//print z            // used before declaration: error
var z := 5

//var w := w         // is w already in scope in its own initializer?