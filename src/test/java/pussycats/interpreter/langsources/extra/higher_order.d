// Closure factory: stateful counter
var makeCounter := func(start, step) is
    var current := start
    return func() is
        var val := current
        current := current + step
        return val
    end
end

var c1 := makeCounter(0, 1)
var c2 := makeCounter(100, 10)

print "c1:", c1(), c1(), c1()
print "c2:", c2(), c2()

// Higher-order function: custom map
var mapArray := func(arr, len, transform) is
    var res := []
    for i in 1..len loop
        res[i] := transform(arr[i])
    end
    return res
end

// Higher-order function: custom filter
var filterArray := func(arr, len, predicate) is
    var res := []
    var count := 0
    for i in 1..len loop
        if predicate(arr[i]) then
            count := count + 1
            res[count] := arr[i]
        end
    end
    return res
end

var nums := [1, 2, 3, 4, 5, 6]
var doubled := mapArray(nums, 6, func(x) => x * 2)
var evens := filterArray(nums, 6, func(x) => (x / 2) * 2 = x)

print "Original:", nums
print "Doubled:", doubled
print "Evens:", evens