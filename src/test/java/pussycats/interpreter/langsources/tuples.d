var person := {name:="Bob", age:=48, nickname:="Robert Paulson"}

// Access by name (dot notation)
print "Name:", person.name

// Access by position (starting from 1)
print "Age (by index):", person.2
print "Nickname (by index):", person.3

// Tuple concatenation (adding a new field)
var extended := person + {phrase:="His name is Robert Paulson."}

// The original tuple is unchanged
print "Original nickname:", person.nickname

// New tuple has the combined fields
print "New phrase:", extended.phrase
print "New tuple size (index 4):", extended.4