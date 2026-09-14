var base := 10

// Lambda captures 'base' variable
var add_base := func(x) => x + base

// Change the outer variable after lambda creation
base := 100

// Prints 105 (captures by reference), NOT 15 (captures by value)
print "Closure result (base changed):", add_base(5)

// Shared mutable state between multiple lambdas
var counter := 0
var increment := func() =>
    counter := counter + 1
    return counter
var reset := func() =>
    counter := 0
    return counter

print "Inc 1:", increment()   // 1
print "Inc 2:", increment()   // 2
print "Reset:", reset()       // 0
print "Inc 3:", increment()   // 1 (shared counter is now 0)