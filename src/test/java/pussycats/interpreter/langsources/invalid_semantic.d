// --- 1. Undeclared Variable ---
print unknown_variable

// --- 2. Duplicate Declaration ---
var x := 1
var x := 2

// --- 3. Invalid Assignment Target ---
5 := 10
1 + 2 := 3

// --- 4. Correct Keyword Usage ---
exit         // Error: Not inside a loop
return 42    // Error: Not inside a function

// --- 5. Duplicate Function Parameters ---
var f := func(a, b, a) => a + b

// --- 6. Duplicate Tuple Keys ---
var t := {name := "Alice", age := 30, name := "Bob"}

// --- 7. Out of Scope Usage ---
if true then
    var inner_var := 5
end
print inner_var