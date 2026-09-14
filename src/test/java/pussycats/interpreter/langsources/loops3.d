var numbers := [42, 15, 88, 7, 23]
var min_val := numbers[1]

for num in numbers loop
    if num < min_val then
        min_val := num
    end
end

print min_val