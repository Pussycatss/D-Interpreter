var x := 10
var y := 52

// Standard if/else
if x > y then
    print "x is greater"
else
    print "y is greater"
end

// Short-form if (no 'end' required)
var z := 5
if z < 10 => print "z less than 10"

// While loop
var counter := 1
while counter <= 3 loop
    print "While iteration:", counter
    counter := counter + 1
end

// Infinite loop with exit
var i := 0
loop
    i := i + 1
    if i = 4 => exit   // Breaks out of the loop
    print "Loop i:", i
end
print "Exited loop at i =", i