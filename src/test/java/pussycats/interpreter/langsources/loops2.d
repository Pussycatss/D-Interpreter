var count := 10
loop
    print "Countdown:", count
    if count = 7 then
        print "Stopped early at 7!"
        exit
    end
    count := count - 1
end

print "Loop finished."