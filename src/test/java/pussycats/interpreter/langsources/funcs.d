var find_index := func(arr, len, target) is
    var i := 1
    while i <= len loop
        if arr[i] = target then
            return i
        end
        i := i + 1
    end
    return -1
end

var data := [10, 25, 42, 99]
print  find_index(data, 4, 42)
print  find_index(data, 4, 100)