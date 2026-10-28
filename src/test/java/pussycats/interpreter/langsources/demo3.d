var data := {name := "Ann", scores := [90, 85, 77], address := {city := "Oslo", zip := 123}}

print data.name
print data.scores[1]
print data.address.city
data.scores[0] := 100
print data.scores is []
