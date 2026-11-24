// Creating objects (tuples with state and functions)
var createAccount := func(owner, initialBalance) is
    var balance := initialBalance
    var history := [initialBalance]
    var txCount := 1

    var depositFn := func(amount) is
        if amount <= 0 => return false
        balance := balance + amount
        txCount := txCount + 1
        history[txCount] := balance
        return true
    end

    var withdrawFn := func(amount) is
        if amount <= 0 or amount > balance => return false
        balance := balance - amount
        txCount := txCount + 1
        history[txCount] := balance
        return true
    end

    var getBalanceFn := func => balance
    var getHistoryFn := func => history

    return {owner := owner, deposit := depositFn, withdraw := withdrawFn, getBalance := getBalanceFn, getHistory := getHistoryFn}
end

var alice := createAccount("Alice", 500)
var bob := createAccount("Bob", 100)

var ok1 := alice.deposit(250)
var ok2 := alice.withdraw(100)
var ok3 := bob.withdraw(500)

print alice.owner, "deposit ok:", ok1, "withdraw ok:", ok2
print bob.owner, "withdraw ok (should be false):", ok3
print alice.owner, "balance:", alice.getBalance()
print alice.owner, "history:", alice.getHistory()
print bob.owner, "balance:", bob.getBalance()

// Array of tuples
var users := [alice, bob]
for u in users loop
    print "User summary:", u.owner, "->", u.getBalance()
end