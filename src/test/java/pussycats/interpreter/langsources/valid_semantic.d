// --- 1. Constant Folding ---
var math := 2 + 3 * 4          // Folds to: 14
var text := "Hello" + " " + "World" // Folds to: "Hello World"
var logic := 5 < 3             // Folds to: false
var neg := -(10 + 5)           // Folds to: -15

// --- 2. Unused Variable Removal ---
var dead_string := "I am never used"
var dead_array := [1, 2, 3, 4]

// --- 3. If Simplification & Unreachable Code ---
if true then
    print "This will always run"
else
    print "This branch is deleted"
end

// --- 4. Empty Loop Removal ---
while false loop
    print "This loop is deleted entirely"
end

// --- 5. Dead Code after Return ---
var safe_divide := func(a, b) is
    if b = 0 then
        return 0
    end
    return a / b
    print "This print is unreachable and deleted"
end

// --- 6. Normal Execution Output ---
print math, text, logic, neg
print safe_divide(10, 2)