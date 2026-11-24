// 'and', 'or' and 'xor' share one precedence level per the grammar
print true or false and false     // grammar: (true or false) and false = false
                                  // C-style precedence would give true

print true xor true xor true      // left-assoc: true

// Relation allows only ONE comparison, so this should be a syntax error
print 1 < 2 < 3

print 1 - -1       // 2
print 2 * -3       // -6
print - -3         // grammar allows only one sign before Primary, so this is questionable
print 10 - 2 - 3   // 5 (left-assoc)
print 100 / 10 / 5 // 2