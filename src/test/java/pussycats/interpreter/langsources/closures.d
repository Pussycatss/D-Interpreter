var x := "global"

func foo() => print x // Which 'x' do we get?

func bar() is
    var x := "local to bar"
    foo() // Call foo from inside bar
end

bar()