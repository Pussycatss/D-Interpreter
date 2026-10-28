var count := 0
var total := 0

while count < 5 loop
    count := count + 1
    if count = 3 => exit
    total := total + count * 2
end

print count, total
