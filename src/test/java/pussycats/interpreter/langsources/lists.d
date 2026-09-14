var scores := [95, 87, 76, 100, 88]
var total := 0

// Iterating over array of elements
for score in scores loop
    total := total + score
end
print "Total score:", total

// Getting element from the array
print "First score:", scores[1]
print "Last score:", scores[5]

// Dynamic array change
scores[10] := 99
print "Score at index 10:", scores[10]

total := 0
// Iterating over array of elements
for score in scores loop
    total := total + score
end
print "New total score:", total