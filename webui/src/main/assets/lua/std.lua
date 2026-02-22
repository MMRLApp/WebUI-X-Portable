-- std.lua

local M = {}

local String  = java.import("java.lang.String")
local Log     = java.import("android.util.Log")
local TAG     = String("Lua")

function M.log(...)
    local parts = {}
    for i = 1, select("#", ...) do
        parts[i] = tostring(select(i, ...))
    end
    local msg = String(table.concat(parts, "\t"))
    Log:d(TAG, msg)
end

_G.print = M.log

return M