// --- 1. Constant Folding ---
var math := 2 + 3 * 4          // Folds to: 14
var text := "Hello" + " " + "World" // Folds to: "Hello World"
var logic := 5 < 3             // Folds to: false
var neg := -(10 + 5)           // Folds to: -15

// --- 2. Literal Indexing & Field Access (NEW) ---
var arr_elem := [10, 20, 30][2]     // Folds to: 20 (1-based index)
var tup_field := {a := "sweetie"}.a // Folds to: "sweetie"

// --- 3. Literal Concatenation (NEW) ---
var arr_cat := [1, 2] + [3, 4]      // Folds to: [1, 2, 3, 4]
var tup_cat := {a := 1} + {b := 2}  // Folds to: {a := 1, b := 2}

// --- 4. If Simplification & Unreachable Code ---
if true then
    print "This will always run"
else
    print "This branch is deleted"
end

// --- 5. Empty Loop Removal ---
while false loop
    print "This loop is deleted entirely"
end

// --- 6. Dead Code after Return ---
var safe_divide := func(a, b) is
    if b = 0 then
        return 0
    end
    return a / b
    print "This print is unreachable and deleted"
end

// --- 7. Unused Variable Removal ---
var dead_string := "I am never used"
var dead_array := [1, 2, 3, 4]

// --- 6. Normal Execution Output ---
print math, text, logic, neg
print arr_elem, tup_field, arr_cat, tup_cat
print safe_divide(10, 2)