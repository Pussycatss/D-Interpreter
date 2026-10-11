var age := 20
var has_ticket := true
var is_vip := false

var can_enter := (age >= 18 and has_ticket) or is_vip
print can_enter

var is_restricted := not can_enter or (age /= 20)
print  is_restricted