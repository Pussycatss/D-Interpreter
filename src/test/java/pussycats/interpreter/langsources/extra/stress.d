// deep recursion: should give a clean error, not crash the host
var down := func(n) is
  if n = 0 then return 0 end
  return 1 + down(n - 1)
end
print down(100000)

// big loop
var s := 0
for i in 1..10000000 loop s := s + i end
print s

// huge string building
var str := ""
for 1..100000 loop str := str + "ab" end

// empty program, only comments, and only separators
//
;;;

// unterminated string
//print "abc

// unknown character
var x := 5 @ 3