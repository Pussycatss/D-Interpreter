var factorial := func(n) is
    if n <= 1 then
        return 1
    else
        return n * factorial(n - 1)
    end
end

print factorial(5)