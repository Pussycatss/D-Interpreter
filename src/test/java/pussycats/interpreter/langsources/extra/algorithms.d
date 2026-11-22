// Bubble Sort implementation
var bubbleSort := func(arr, len) is
    for i in 1..len loop
        var swapped := false
        for j in 1..(len - i) loop
            if arr[j] > arr[j + 1] then
                var temp := arr[j]
                arr[j] := arr[j + 1]
                arr[j + 1] := temp
                swapped := true
            end
        end
        if not swapped => exit
    end
    return arr
end

// Binary Search implementation
var binarySearch := func(arr, len, target) is
    var low := 1
    var high := len
    while low <= high loop
        var mid := (low + high) / 2
        if arr[mid] = target => return mid
        if arr[mid] < target then
            low := mid + 1
        else
            high := mid - 1
        end
    end
    return -1
end

var numbers := [64, 34, 25, 12, 22, 11, 90]
print "Before sort:", numbers

var sorted := bubbleSort(numbers, 7)
print "After sort:", sorted

print "Index of 25:", binarySearch(sorted, 7, 25)
print "Index of 90:", binarySearch(sorted, 7, 90)
print "Index of 99 (missing):", binarySearch(sorted, 7, 99)