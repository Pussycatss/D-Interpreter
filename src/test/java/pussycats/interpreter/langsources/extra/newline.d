var a := 1
-2                    // new statement or continuation of "1 - 2"?
print a

var b := 5
(b)                   // continuation as a call, or new statement?

var q := [1,2,3]
[0]                   // continuation as indexing, or new statement?

print 1,
  2                   // newline after a comma
print 1
,2                    // newline before a comma

var u := 1, v := 2 print u   // no separator at all
var s := 1;;;  print s       // empty statements