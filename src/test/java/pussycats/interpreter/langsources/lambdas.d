// Simple lambda
var square := func(x) => x * x
print "Square of 7:", square(7)

// Lambda with multiple expressions (if using 'return' is allowed)
var absolute := func(n) is
    if n < 0 then
        return -n
    else
        return n
    end
end
print "Absolute of -5:", absolute(-5)

// Higher-order: lambda that takes a lambda
var apply_twice := func(f, val) => f(f(val))
print "Double square of 3:", apply_twice(square, 3)