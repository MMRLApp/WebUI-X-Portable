-- std.lua

_G.String                       = java.import("java.lang.String")
_G.ByteArrayInputStream         = java.import("java.io.ByteArrayInputStream")
_G.ByteArrayOutputStream        = java.import("java.io.ByteArrayOutputStream")
_G.BufferedInputStream          = java.import("java.io.BufferedInputStream")
_G.Array                        = java.import("java.lang.reflect.Array")
_G.Byte                         = java.import("java.lang.Byte")
_G.Object                       = java.import("java.lang.Object")

_G.context                      = _WX_OPTIONS:getContext()
-- android.net.Uri
_G.domain                       = _WX_OPTIONS:getDomain()

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