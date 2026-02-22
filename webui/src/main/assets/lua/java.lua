-- java.lua
-- Java interop and Android logging

local M = {}

-- luajava

M.luajava = {
    newInstance = function(className, ...)
        return java.import(className)(...)
    end,
    bindClass   = java.import,
    new         = java.new,
    createProxy = java.proxy,
    loadLib     = function(className, methodName)
        return java.loadlib(className, methodName)()
    end,
}

-- Android Logging

local AndroidLog = M.luajava.bindClass("android.util.Log")
local TAG        = M.luajava.newInstance("java.lang.String", "Lua")

function M.log(...)
    local parts = {}
    for i = 1, select("#", ...) do
        parts[i] = tostring(select(i, ...))
    end
    local msg = M.luajava.newInstance("java.lang.String", table.concat(parts, "\t"))
    AndroidLog:d(TAG, msg)
end

_G.print = M.log

return M