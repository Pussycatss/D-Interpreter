for i in 3..1 loop print i end        // empty range, or counting down?
for i in 1..1 loop print i end        // single iteration
for i in 1.5..4.2 loop print i end    // real bounds: truncate, or error?

// the range is evaluated once
var lim := 3
for i in 1..lim loop lim := 10; print i end   // 1 2 3 only

// modifying the loop variable inside the body
for i in 1..3 loop i := i * 10; print i end   // does the iterator see the change?

// nested loops: exit leaves only the inner loop
for i in 1..3 loop
  for j in 1..3 loop
    if j = 2 => exit
    print i, j
  end
end

exit                                  // outside a loop: static error
var ff := func => exit                // exit inside a function but outside a loop
for 1..2 loop ff(1) end               // exit must not cross the function boundary

// "return" at top level
return 5

// non-boolean condition
while 1 loop print "x"; exit end

// body missing 'end', or 'end' with no 'loop'
for 1..3 loop print 1

// mutate the array while iterating over it
var arr := [1, 2, 3]
for x in arr loop
  if x = 1 => arr[4] := 99
  print x
end