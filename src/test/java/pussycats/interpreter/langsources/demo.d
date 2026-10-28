var x := 1 + 2 * 3
var name := "world"

if x > 5 then
    print "big"
else
    print "small"
end

var double := func(n) => n * 2

for i in [1, 2, 3] loop
    print double(i)
end

var p := {x := 1, y := 2.5}
print name, p.x