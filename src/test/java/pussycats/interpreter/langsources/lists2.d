var double_val := func(n) => n * 2

var arr := [1, 2, 3, 4]
var idx := 1

while idx <= 4 loop
    arr[idx] := double_val(arr[idx])
    idx := idx + 1
end

print  arr[1]
print  arr[4]