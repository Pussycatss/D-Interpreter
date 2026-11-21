var a
print a is none    // true
print a            // is print allowed? "only action is the is-check"
print a + 1        // runtime error
print a = a        // error or true?

var b := none
print b is none
var c := [none, none]
print c[1] is none
var arr := []
print arr[5] is none   // missing key: none or error?