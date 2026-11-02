// --- 1. NameError ---
print unknown_variable

// --- 2. DeclarationError ---
var x := 1
var x := 2

// --- 3. ReferenceError (Invalid L-Value) ---
5 := 10
1 + 2 := 3

// --- 4. ContextError ---
exit         // Error: Not inside a loop
return 42    // Error: Not inside a function

// --- 5. DeclarationError (Duplicates) ---
var f := func(a, b, a) => a + b
var t := {name := "Alice", age := 30, name := "Bob"}

// --- 6. NameError (Out of Scope) ---
if true then
    var inner_var := 5
end
print inner_var

// --- 7. IndexError (Tuple Bounds) ---
var my_tuple := {a := 1, b := 2}
print my_tuple.c
print my_tuple.3
print {x := 1}.y