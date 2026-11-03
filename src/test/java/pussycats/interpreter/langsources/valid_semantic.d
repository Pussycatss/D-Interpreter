// --- 1. Constant Folding (Arithmetic, Strings, Logic) ---
var math := 2 + 3 * 4                                      // Folds to: 14
var text := "Compilers" + " " + "Construction"             // Folds to: "Compilers Construction"
var logic := 5 < 3                                         // Folds to: false
var neg := -(10 + 5)                                       // Folds to: -15

// --- 2. Literal Aggregate Indexing & Field Access ---
var arr_elem := [10, 20, 30][2]                            // Folds to: 20 (1-based index)
var tup_elem := {10, 20, 30}.3                             // Folds to: 30
var tup_field := {a := "apple", b := "pen"}.a              // Folds to: "apple"

// --- 3. Literal Aggregate Concatenation ---
var arr_cat := [1, 2] + [3, 4]                             // Folds to: [1, 2, 3, 4]
var tup_cat := {a := 1} + {b := 2}                         // Folds to: {a := 1, b := 2}

// --- 4. If Simplification ---
if true then
    print "The king is dead."
else
    print "Long live the king!"
end

// --- 5. Empty Loop Removal ---
while false loop
    print "This goes hard on mute"
end

// --- 6. Dead Code after Return ---
var safe_divide := func(a, b) is
    if b = 0 then
        return 0
    end
    return a / b
    print "Nothing"
end

// --- 7. Unused Variable Removal ---
var dead_string := "What is my purpose?"
var dead_array := [1, 2, 3, 4]

// --- 6. Normal Execution Output ---
print math, text, logic, neg
print arr_elem, tup_elem, tup_field
print arr_cat, tup_cat
print safe_divide(10, 2)