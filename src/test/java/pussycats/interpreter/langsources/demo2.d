var square := func(x) => x * x

var apply := func(f, v) is
    return f(v)
end

var result := apply(square, 7)
print result
