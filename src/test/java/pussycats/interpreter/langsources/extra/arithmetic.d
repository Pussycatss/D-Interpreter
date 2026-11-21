print 7 / 2        // 3
print -7 / 2       // "round down" means -4, but C-style truncation gives -3
print 7 / 2.0      // 3.5
print 1 / 0        // runtime error
print 1.0 / 0.0    // error, or inf? the spec is silent
print 0.1 + 0.2 = 0.3   // false (float behaviour)
print 1 = 1.0           // true per the Integer/Real comparison rule
print 9223372036854775807 + 1   // overflow: wrap, error, or bigint?